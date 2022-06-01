package server;

import communication.CommunicationUtils;
import communication.IPAddress;
import communication.MulticastHandler;
import message.JoinMessage;
import message.Message;
import message.PutRelayMessage;
import message.PutRelayReply;
import server.state.InitNodeState;
import server.state.NodeState;
import server.tasks.ElectionTask;
import server.tasks.MembershipTask;
import server.tasks.MessageReceiverTask;
import utils.*;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MembershipService implements MembershipRMI {
    public static final int REPLICATION_FACTOR = 3;
    public final Object joinLeaveLock = new Object();
    private final StorageService storageService;
    private final MembershipCounter nodeMembershipCounter;
    private final MembershipLog membershipLog;
    private final ClusterMap clusterMap;
    private final SentMemberships sentMemberships = new SentMemberships();
    private final IPAddress ipMulticastGroup;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final MessageReceiverTask messageReceiverTask;
    private NodeState nodeState;
    private MulticastHandler multicastHandler;

    protected MembershipService(StorageService storageService) {
        this.storageService = storageService;
        this.ipMulticastGroup = null;
        this.nodeMembershipCounter = new MembershipCounter(getMembershipCounterFilePath());
        this.membershipLog = new MembershipLog(getMembershipLogFilePath());
        this.clusterMap = new ClusterMap(getClusterMapFilePath());
        this.messageReceiverTask = null;
    }

    public MembershipService(StorageService storageService, IPAddress ipMulticastGroup, ServerSocket socket) {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.nodeMembershipCounter = new MembershipCounter(getMembershipCounterFilePath());
        this.membershipLog = new MembershipLog(getMembershipLogFilePath());
        this.clusterMap = new ClusterMap(getClusterMapFilePath());
        this.nodeState = new InitNodeState(this);
        this.messageReceiverTask = new MessageReceiverTask(this, socket);
        Thread messageReceiverThread = new Thread(this.messageReceiverTask);
        messageReceiverThread.start();

        if (isJoined()) {
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

    public MessageReceiverTask getMessageReceiverTask() {
        return messageReceiverTask;
    }

    public void initMulticastHandler() throws IOException {
        this.multicastHandler = new MulticastHandler(storageService.getNode(), ipMulticastGroup, this);
    }

    public SentMemberships getSentMemberships() {
        return sentMemberships;
    }

    public Map<String, Integer> getMembershipLog(int numberOfLogs) {
        return membershipLog.getMostRecentLogs(numberOfLogs);
    }

    public NodeState getNodeState() {
        return nodeState;
    }

    public void setNodeState(NodeState nodeState) {
        this.nodeState = nodeState;
    }

    /**
     * @return the full membership log map.
     */
    public MembershipLog getMembershipLog() {
        return membershipLog;
    }

    public JoinMessage createJoinMessage(int port) {
        JoinMessage joinMessage = new JoinMessage();
        joinMessage.setCounter(nodeMembershipCounter.get());
        joinMessage.setNodeId(storageService.getNode().id());
        joinMessage.setConnectionPort(port);
        joinMessage.setPort(storageService.getNode().port());
        return joinMessage;
    }

    @Override
    public boolean join() {
        return nodeState.join();
    }

    @Override
    public boolean leave() {
        return nodeState.leave();
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

    public boolean isLeader() {
        return this.isLeader.get();
    }

    public void setLeader(boolean isLeader) {
        this.isLeader.set(isLeader);
    }

    public void sendToNextAvailableNode(Message message) {
        Node currentNode = getStorageService().getNode();
        while (true) {
            Node nextNode = getClusterMap().getNodeSuccessor(currentNode);
            if (!CommunicationUtils.dispatchMessageToNodeWithoutReply(nextNode, message)) {
                currentNode = nextNode;
            } else {
                return;
            }
        }
    }

    public void transferKeysToJoiningNode(Node joiningNode) {
        String joiningNodeHash = StoreUtils.sha256(joiningNode.id().getBytes(StandardCharsets.UTF_8));
        String thisNodeHash = StoreUtils.sha256(this.getStorageService()
                .getNode().id().getBytes(StandardCharsets.UTF_8));
        PutRelayMessage putMessage = new PutRelayMessage();

        for (String hash : this.getStorageService().getHashes()) {
            boolean hashIsLessThanJoiningNode = hash.compareTo(joiningNodeHash) <= 0;
            boolean hashIsHigherThanThisNode = hash.compareTo(thisNodeHash) >= 0;
            boolean mustTransferHash =
                    joiningNodeHash.compareTo(thisNodeHash) < 0
                            ? hashIsLessThanJoiningNode || hashIsHigherThanThisNode
                            : hashIsLessThanJoiningNode && hashIsHigherThanThisNode;
            boolean mustReplicateHash =
                    clusterMap.getNodesResponsibleForHash(hash, REPLICATION_FACTOR + 1).size() <= REPLICATION_FACTOR;

            if (!mustTransferHash && !mustReplicateHash) {
                continue;
            }
            System.out.println("Transferring key " + hash + " to joining node " + joiningNode.id());

            try {
                File file = new File(this.getStorageService().getValueFilePath(hash));
                byte[] bytes = Files.readAllBytes(file.toPath());
                putMessage.addValue(hash, bytes);
            } catch (IOException e) {
                throw new IllegalArgumentException("File not found");
            }
        }

        if (putMessage.getValues().isEmpty()) {
            return;
        }

        try {
            PutRelayReply putReply = (PutRelayReply) CommunicationUtils.dispatchMessageToNode(joiningNode, putMessage, null);
            if (putReply == null) {
                return;
            }
            for (String hash : putReply.getSuccessfulHashes()) {
                if (clusterMap.getNodesResponsibleForHash(hash, REPLICATION_FACTOR + 1).size() == REPLICATION_FACTOR + 1) {
                    this.getStorageService().delete(hash);
                }
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    public void transferAllMyKeysToNewSuccessors() {
        for (String hash : getStorageService().getHashes()) {
            try {
                File file = new File(getStorageService().getValueFilePath(hash));
                byte[] bytes = Files.readAllBytes(file.toPath());
                PutRelayMessage putMessage = new PutRelayMessage();
                putMessage.addValue(hash, bytes);
                List<Node> responsibleNodes = clusterMap.getNodesResponsibleForHash(hash, MembershipService.REPLICATION_FACTOR + 1);
                for (Node node : responsibleNodes) {
                    if (node.id().equals(this.getStorageService().getNode().id())) {
                        continue;
                    }
                    PutRelayReply putReply = (PutRelayReply) CommunicationUtils.dispatchMessageToNode(node, putMessage, null);
                    if (putReply == null) {
                        // TODO: calculate replication again
                        continue;
                    }
                    for (String deletedHash : putReply.getSuccessfulHashes()) {
                        this.getStorageService().delete(deletedHash);
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("File not found");
            }
        }
    }

    public void removeUnavailableNode(Node node) {
        Integer nodeCounter = this.membershipLog.get(node.id());
        if (nodeCounter != null) {
            this.membershipLog.put(node.id(), nodeCounter + 1);
        }
        this.clusterMap.remove(node);
        System.out.println(node + " is unavailable. Removed from cluster map");
    }
}
