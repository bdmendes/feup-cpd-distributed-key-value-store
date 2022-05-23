package server;

import communication.IPAddress;
import communication.JoinInitMembership;
import communication.MulticastHandler;
import message.*;
import utils.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.*;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final MembershipCounter nodeMembershipCounter;
    private final MembershipLog membershipLog;
    private final ClusterMap clusterMap;
    private final IPAddress ipMulticastGroup;
    private final SentMemberships sentMemberships = new SentMemberships();
    private MulticastHandler multicastHandler;

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.nodeMembershipCounter = new MembershipCounter(getMembershipCounterFilePath());
        this.membershipLog = new MembershipLog(getMembershipLogFilePath());
        this.clusterMap = new ClusterMap(getClusterMapFilePath());

        if (ipMulticastGroup != null && isJoined()) {
            multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
            Thread multicastHandlerThread = new Thread(multicastHandler);
            multicastHandlerThread.start();
        }
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
            MessageProcessor.dispatchMessageToNode(successorNode, putMessage, null);
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
}
