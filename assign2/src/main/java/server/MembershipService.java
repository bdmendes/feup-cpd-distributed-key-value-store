package server;

import communication.CommunicationUtils;
import communication.IPAddress;
import communication.JoinInitMembership;
import communication.MulticastHandler;
import message.JoinMessage;
import message.Message;
import message.PutMessage;
import server.tasks.ElectionTask;
import server.tasks.MembershipTask;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final MembershipCounter nodeMembershipCounter;
    private final MembershipLog membershipLog;
    private final ClusterMap clusterMap;
    private final IPAddress ipMulticastGroup;
    private final SentMemberships sentMemberships = new SentMemberships();
    private MulticastHandler multicastHandler;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.nodeMembershipCounter = new MembershipCounter(getMembershipCounterFilePath());
        this.membershipLog = new MembershipLog(getMembershipLogFilePath());
        this.clusterMap = new ClusterMap(getClusterMapFilePath());

        if (ipMulticastGroup != null && isJoined()) {
            clusterMap.clear();
            membershipLog.clear();
            nodeMembershipCounter.incrementAndGet();
            join();
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ElectionTask electionTask = new ElectionTask(this);
        scheduler.scheduleAtFixedRate(electionTask, 0, 10, TimeUnit.SECONDS);
        MembershipTask membershipTask = new MembershipTask(this);
        scheduler.scheduleAtFixedRate(membershipTask, 0, 1, TimeUnit.SECONDS);
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

    public MembershipCounter getMembershipCounter() {
        return nodeMembershipCounter;
    }

    public MulticastHandler getMulticastHandler() {
        return multicastHandler;
    }

    public SentMemberships getSentMemberships() {
        return sentMemberships;
    }

    public Map<String, Integer> getMembershipLog(int numberOfLogs) {
        return membershipLog.getMostRecentLogs(numberOfLogs);
    }

    /**
     * @return the full membership log map.
     */
    public MembershipLog getMembershipLog() {
        return membershipLog;
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

        int counter = nodeMembershipCounter.incrementAndGet();

        JoinMessage message = createJoinMessage(serverSocket.getLocalPort());
        JoinInitMembership messageReceiver = new JoinInitMembership(this, serverSocket, message, multicastHandler, 2000);
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

        clusterMap.put(storageService.getNode());
        membershipLog.put(storageService.getNode().id(), counter);

        System.out.println(this.getClusterMap().getNodes());
        System.out.println(this.getMembershipLog(32));

        Thread multicastHandlerThread = new Thread(multicastHandler);
        multicastHandlerThread.start();

        return true;
    }

    private void transferAllMyKeysToMySuccessor() {
        Node successorNode = clusterMap.getNodeSuccessor(this.storageService.getNode());
        if (successorNode.equals(this.storageService.getNode())) {
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
            CommunicationUtils.dispatchMessageToNode(successorNode, putMessage, null);
            this.getStorageService().delete(hash);
        }
    }

    @Override
    public boolean leave() throws IOException {
        if (!isJoined()) {
            return false;
        }

        try {
            nodeMembershipCounter.incrementAndGet();
            this.multicastJoinLeave(-1);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        multicastHandler.close();

        this.transferAllMyKeysToMySuccessor();

        clusterMap.clear();
        membershipLog.clear();

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

    private String getClusterMapFilePath() {
        return "./node_storage/storage" + storageService.getNode() + "/cluster_map.txt";
    }

    public void setLeader(boolean isLeader) {
        this.isLeader.set(isLeader);
    }

    public boolean isLeader() {
        return this.isLeader.get();
    }

    public void sendToNextAvailableNode(Message message) {
        Node currentNode = getStorageService().getNode();
        Node nextNode;
        boolean notSent = true;

        while (notSent) {
            nextNode = getClusterMap().getNodeSuccessor(currentNode);
            try {
                CommunicationUtils.dispatchMessageToNodeWithoutReply(nextNode, message);
                notSent = false;
            } catch (RuntimeException e) {
                currentNode = nextNode;
            }
        }
    }

}
