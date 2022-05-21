package server;

import communication.IPAddress;
import communication.MessageReceiver;
import communication.MessageSender;
import communication.MulticastSender;
import message.*;
import utils.MembershipLog;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MembershipService implements MembershipRMI {
    private final StorageService storageService;
    private final AtomicInteger nodeMembershipCounter = new AtomicInteger();
    private final Map<String, Integer> membershipLog = MembershipLog.generateMembershipLog();
    private final Set<Node> clusterNodes;
    private final IPAddress ipMulticastGroup;
    private final ServerSocket serverSocket;

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup) throws IOException {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.serverSocket = new ServerSocket(ipMulticastGroup.getPort());
        clusterNodes = ConcurrentHashMap.newKeySet();
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
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
        try {
            Scanner scanner = new Scanner(new File(getMembershipCounterFilePath()));
            nodeMembershipCounter.set(scanner.nextInt());
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

    private boolean multicastJoinLeave(ServerSocket serverSocket) throws IOException {
        JoinMessage message = new JoinMessage();
        message.setCounter(nodeMembershipCounter.get());
        message.setNodeId(storageService.getNode().id());
        incrementCounter();
        MulticastSender multicastSender = new MulticastSender(message, ipMulticastGroup);
        MessageReceiver messageReceiver = new MessageReceiver(serverSocket, 200);
        multicastSender.sendMessage();
        for (int i = 0; i < 3; i++){
            Message receivedMessage = messageReceiver.receiveMessage();
            if (receivedMessage == null) {
                if (i == 2){
                    return true;
                }
                multicastSender.sendMessage();
                continue;
            }
            MessageProcessor processor = new MessageProcessor(this, receivedMessage, null);
            processor.run();
        }
        return true;
    }

    @Override
    public boolean join() throws IOException {
        if (nodeMembershipCounter.get() % 2 != 0) {
            return false;
        }
        clusterNodes.add(storageService.getNode());
        addMembershipEvent(storageService.getNode().id(),
                nodeMembershipCounter.get() == 0
                        ? nodeMembershipCounter.getAndIncrement()
                        : nodeMembershipCounter.incrementAndGet());
        return this.multicastJoinLeave(serverSocket);
    }

    @Override
    public boolean leave() throws IOException {
        if (nodeMembershipCounter.get() % 2 == 0) {
            return false;
        }
        clusterNodes.remove(storageService.getNode());
        addMembershipEvent(storageService.getNode().id(), nodeMembershipCounter.incrementAndGet());
        return this.multicastJoinLeave(serverSocket);
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
