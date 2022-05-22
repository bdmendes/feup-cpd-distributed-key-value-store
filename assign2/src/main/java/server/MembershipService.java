package server;

import communication.IPAddress;
import communication.JoinInitMembership;
import communication.MulticastHandler;
import message.*;
import message.messagereader.MessageReader;
import utils.MembershipLog;
import utils.SentMemberships;
import utils.StoreUtils;

import java.io.*;
import java.lang.reflect.Member;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final AtomicInteger nodeMembershipCounter = new AtomicInteger(-1);
    private final MembershipLog membershipLog = new MembershipLog();
    private final ClusterMap clusterMap = new ClusterMap();
    private final IPAddress ipMulticastGroup;
    private final SentMemberships sentMemberships = new SentMemberships();
    private MulticastHandler multicastHandler;
    private boolean isLeader;
    private final ScheduledExecutorService scheduler;
    private final ElectionTask electionTask = new ElectionTask(this);

    protected MembershipService(StorageService storageService) {
        this.storageService = storageService;
        ipMulticastGroup = null;
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
        scheduler = null;
    }

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
        this.isLeader = false;

        if (isJoined()) {
            multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
            Thread multicastHandlerThread = new Thread(multicastHandler);
            multicastHandlerThread.start();
        }

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(electionTask,0, 10, TimeUnit.SECONDS);
    }

    public boolean isJoined() {
        return nodeMembershipCounter.get() % 2 == 0;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public ClusterMap getClusterMap() {
        return clusterMap;
    }

    public IPAddress getIpMulticastGroup() {
        return ipMulticastGroup;
    }

    public SentMemberships getSentMemberships() {
        return sentMemberships;
    }

    public Map<String, Integer> getMembershipLog(int numberOfLogs) {
        return membershipLog.getMostRecentLogs(numberOfLogs);
    }


    public Map<String, Integer> cloneLog() {
        return new LinkedHashMap<>(membershipLog.getMap());
    }

    /**
     * ONLY use get with this map.
     * @return the full membership log map.
     */
    public MembershipLog getMembershipLog() {
        return membershipLog;
    }

    protected void readMembershipCounterFromFile() {
        int counter;
        try {
            Scanner scanner = new Scanner(new File(getMembershipCounterFilePath()));
            counter = scanner.nextInt();
            nodeMembershipCounter.set(counter);
            scanner.close();
        } catch (Exception e) {
            nodeMembershipCounter.set(-1);
            this.writeMembershipCounterToFile(-1);
        }
    }

    protected void writeMembershipCounterToFile(int counter) {
        try {
            FileWriter fileWriter = new FileWriter(getMembershipCounterFilePath());
            fileWriter.write(Integer.toString(counter));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void readMembershipLogFromFile() {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(getMembershipLogFilePath()));
            MembershipLog.readMembershipLogFromData(membershipLog.getMap(), bytes);
        } catch (IOException e) {
            this.writeMembershipLogToFile();
        }
    }

    protected void writeMembershipLogToFile() {
        byte[] bytes = MembershipLog.writeMembershipLogToData(membershipLog.getMap());
        try (FileOutputStream fos = new FileOutputStream(getMembershipLogFilePath())) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JoinMessage createJoinMessage(int port) {
        JoinMessage joinMessage = new JoinMessage();
        joinMessage.setCounter(nodeMembershipCounter.get());
        joinMessage.setNodeId(storageService.getNode().id());
        joinMessage.setConnectionPort(port);
        joinMessage.setPort(storageService.getNode().port());
        return joinMessage;
    }

    private void multicastJoinLeave(int port) throws IOException {
        JoinMessage message = createJoinMessage(port);
        multicastHandler.sendMessage(message);
    }

    @Override
    public boolean join() throws IOException {
        if (isJoined()) {
            return false;
        }

        multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(storageService.getNode().id(), 0));

        int counter = incrementAndGetCounter();

        JoinMessage message = createJoinMessage(serverSocket.getLocalPort());
        JoinInitMembership messageReceiver = new JoinInitMembership(this, serverSocket, message, multicastHandler,2000);
        Thread messageReceiverThread = new Thread(messageReceiver);
        messageReceiverThread.start();

        try {
            this.multicastJoinLeave(serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
            multicastHandler.close();
            messageReceiver.close();
            return false;
        }

        clusterMap.add(storageService.getNode());
        addMembershipEvent(storageService.getNode().id(), counter);

        System.out.println(this.getClusterMap().getNodes());
        System.out.println(this.getMembershipLog(32));

        Thread multicastHandlerThread = new Thread(multicastHandler);
        multicastHandlerThread.start();

        return true;
    }

    private void transferAllMyKeysToMySuccessor() {
        Node successorNode = clusterMap.getNodeSuccessor(this.storageService.getNode());
        if (successorNode.equals(this.storageService.getNode())){
            return;
        }

        for (String hash : getStorageService().getHashes()) {
            PutMessage putMessage = new PutMessage();
            try {
                File file = new File(getStorageService().getValueFilePath(hash));
                byte[] bytes = Files.readAllBytes(file.toPath());
                String key = StoreUtils.sha256(bytes);
                putMessage.setKey(key);
                putMessage.setValue(bytes);
            } catch (IOException e) {
                throw new IllegalArgumentException("File not found");
            }
            dispatchMessageToNodeWithoutReply(successorNode, putMessage);
            this.getStorageService().delete(hash);
        }
    }

    @Override
    public boolean leave() throws IOException {
        if (!isJoined()) {
            return false;
        }

        try {
            incrementAndGetCounter();
            this.multicastJoinLeave(-1);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        multicastHandler.close();

        this.transferAllMyKeysToMySuccessor();

        clusterMap.clear();
        clearMembershipLog();

        return true;
    }

    public int getNodeMembershipCounter() {
        return nodeMembershipCounter.get();
    }


    private String getMembershipCounterFilePath() {
        return "./node_storage/storage" + storageService.getNode() + "/membership_counter.txt";
    }

    private String getMembershipLogFilePath() {
        return "./node_storage/storage" + storageService.getNode() + "/membership_log.txt";
    }

    protected void addMembershipEvent(String nodeId, int membershipCounter){
        membershipLog.put(nodeId, membershipCounter);
        this.writeMembershipLogToFile();
    }

    protected void clearMembershipLog(){
        membershipLog.clear();
        this.writeMembershipLogToFile();
    }

    protected int incrementAndGetCounter() {
        int counter = this.nodeMembershipCounter.incrementAndGet();
        this.writeMembershipCounterToFile(counter);
        return counter;
    }

    public void setLeader() {
        this.isLeader = true;
    }

    public void unsetLeader() {
        this.isLeader = false;
    }

    public boolean isLeader() {
        return this.isLeader;
    }

    public void sendMessage(Message message, Socket socket) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(message.encode());
            dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not send message");
        }
    }

    public Message readMessage(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            MessageReader messageReader = new MessageReader();
            while (!messageReader.isComplete()) {
                messageReader.read(bufferedReader);
            }
            return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
        } catch (IOException e) {
            throw new RuntimeException("Could not read message");
        }
    }

    public void dispatchMessageToNode(Node node, Message message, Socket clientSocket) {
        try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())){
            sendMessage(message, responsibleNodeSocket);
            Message replyMessage = readMessage(responsibleNodeSocket);
            if (clientSocket != null) {
                System.out.println("Sending dispatched request back to the client");
                sendMessage(replyMessage, clientSocket);
            }
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    public void dispatchMessageToNodeWithoutReply(Node node, Message message) {
        try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())){
            sendMessage(message, responsibleNodeSocket);
        } catch (IOException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    public void sendToNextAvailableNode(Message message) {
        Node currentNode = getStorageService().getNode();
        Node nextNode;
        boolean notSent = true;

        while(notSent) {
            nextNode = getClusterMap().getNodeSuccessor(currentNode);
            try {
                dispatchMessageToNodeWithoutReply(nextNode, message);
                notSent = false;
            } catch (RuntimeException e) {
                currentNode = nextNode;
            }
        }
    }

}
