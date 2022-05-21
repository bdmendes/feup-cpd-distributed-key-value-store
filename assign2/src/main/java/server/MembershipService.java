package server;

import communication.IPAddress;
import communication.MessageReceiver;
import communication.MulticastHandler;
import message.*;
import utils.MembershipLog;

import java.io.*;
import java.lang.reflect.Member;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final AtomicInteger nodeMembershipCounter = new AtomicInteger();
    private final Map<String, Integer> membershipLog;
    private final Set<Node> clusterNodes;
    private final IPAddress ipMulticastGroup;
    private MulticastHandler multicastHandler;

    protected MembershipService(StorageService storageService) {
        this.storageService = storageService;
        membershipLog = MembershipLog.generateMembershipLog();
        clusterNodes = ConcurrentHashMap.newKeySet();
        ipMulticastGroup = null;
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
    }

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.membershipLog = MembershipLog.generateMembershipLog();
        clusterNodes = ConcurrentHashMap.newKeySet();
        clusterNodes.add(storageService.getNode());
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();

        if(nodeMembershipCounter.get() % 2 != 0) {
            multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
            Thread multicastHandlerThread = new Thread(multicastHandler);
            multicastHandlerThread.start();
        }
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public Set<Node> getClusterNodes() {
        return clusterNodes;
    }

    public IPAddress getIpMulticastGroup() {
        return ipMulticastGroup;
    }

    public Map<String, Integer> getMembershipLog() {
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
            nodeMembershipCounter.set(0);
            this.writeMembershipCounterToFile(0);
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
            MembershipLog.readMembershipLogFromData(membershipLog, bytes);
        } catch (IOException e) {
            this.writeMembershipLogToFile();
        }
    }

    protected void writeMembershipLogToFile() {
        byte[] bytes = MembershipLog.writeMembershipLogToData(this.membershipLog);
        try (FileOutputStream fos = new FileOutputStream(getMembershipLogFilePath())) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void multicastMembershipLog() {
        //
    }

    private void multicastJoinLeave(int port) throws IOException {
        JoinMessage message = new JoinMessage();
        message.setCounter(nodeMembershipCounter.get());
        message.setNodeId(storageService.getNode().id());
        message.setPort(port);
        multicastHandler.sendMessage(message);
        incrementCounter();
    }

    @Override
    public boolean join() throws IOException {
        if (nodeMembershipCounter.get() % 2 != 0) {
            return false;
        }

        multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(storageService.getNode().id(), 0));

        MessageReceiver messageReceiver = new MessageReceiver(this, serverSocket, 1200);
        Thread messageReceiverThread = new Thread(messageReceiver);
        messageReceiverThread.start();

        try {
            this.multicastJoinLeave(serverSocket.getLocalPort());
        } catch (IOException e) {
            e.printStackTrace();
            multicastHandler.close();
            return false;
        }

//        MessageReceiver messageReceiver = new MessageReceiver(serverSocket, 200);
//        multicastSender.sendMessage();
//        for (int i = 0; i < 3; i++){
//            Message receivedMessage = messageReceiver.receiveMessage();
//            if (receivedMessage == null) {
//                if (i == 2){
//                    return true;
//                }
//                multicastSender.sendMessage();
//                continue;
//            }
//            MessageProcessor processor = new MessageProcessor(this, receivedMessage, null);
//            processor.run();
//        }
        Thread multicastHandlerThread = new Thread(multicastHandler);
        multicastHandlerThread.start();

        return true;
    }

    @Override
    public boolean leave() throws IOException {
        if (nodeMembershipCounter.get() % 2 == 0) {
            return false;
        }

        try {
            this.multicastJoinLeave(-1);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        multicastHandler.close();
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

    protected void incrementCounter() {
        int counter = this.nodeMembershipCounter.incrementAndGet();
        this.writeMembershipCounterToFile(counter);
    }
}
