package server;

import communication.IPAddress;
import communication.JoinInitMembership;
import communication.MulticastHandler;
import message.*;
import utils.MembershipLog;
import utils.SentMemberships;

import java.io.*;
import java.lang.reflect.Member;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final AtomicInteger nodeMembershipCounter = new AtomicInteger(-1);
    private final MembershipLog membershipLog = new MembershipLog();
    private final ClusterMap clusterMap = new ClusterMap();
    private final IPAddress ipMulticastGroup;
    private final SentMemberships sentMemberships = new SentMemberships();
    private MulticastHandler multicastHandler;

    protected MembershipService(StorageService storageService) {
        this.storageService = storageService;
        ipMulticastGroup = null;
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
    }

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();

        if (isJoined()) {
            multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
            Thread multicastHandlerThread = new Thread(multicastHandler);
            multicastHandlerThread.start();
        }
    }

    private boolean isJoined() {
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

    private void multicastMembershipLog() {
        //
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

        clusterMap.clear();
        clearMembershipLog();

        System.out.println(this.getClusterMap().getNodes());
        System.out.println(this.getMembershipLog(32));
        
        // TRANSFER ALL MY KEYS TO MY SUCCESSOR

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
}
