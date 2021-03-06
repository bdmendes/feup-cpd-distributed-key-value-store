package server;

import communication.CommunicationUtils;
import communication.IPAddress;
import communication.MulticastHandler;
import message.*;
import server.state.InitNodeState;
import server.state.NodeState;
import server.tasks.ElectionTask;
import server.tasks.MembershipTask;
import server.tasks.MessageReceiverTask;
import utils.ClusterMap;
import utils.MembershipCounter;
import utils.MembershipLog;
import utils.SentMemberships;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.*;
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
        this.nodeState = new InitNodeState(this);
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

    public JoinMessage createJoinMessage(int port, int counter) {
        JoinMessage joinMessage = new JoinMessage();
        joinMessage.setCounter(counter);
        joinMessage.setNodeId(storageService.getNode().id());
        joinMessage.setConnectionPort(port);
        joinMessage.setPort(storageService.getNode().port());
        return joinMessage;
    }

    @Override
    public MembershipRMI.Status join() {
        return nodeState.join();
    }

    @Override
    public MembershipRMI.Status leave() {
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
                removeUnavailableNode(nextNode, true);
                currentNode = nextNode;
            } else {
                return;
            }
        }
    }

    private List<String> sendPutRelayMessageToNode(Node node, PutRelayMessage putMessage) {
        if (putMessage.getValues().size() == 0) {
            return new ArrayList<>();
        }

        try {
            PutRelayReply putReply = (PutRelayReply) CommunicationUtils.dispatchMessageToNode(node, putMessage, null);
            if (putReply == null) {
                return new ArrayList<>();
            }

            return putReply.getSuccessfulHashes();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    public void orderJoiningNodeToDeleteMyTombstones(Node joiningNode) {
        synchronized (this.storageService.getTombstones()) {
            for (String hash : storageService.getTombstones()) {
                DeleteRelayMessage message = new DeleteRelayMessage();
                message.setTransference(true);
                message.setKey(hash);
                CommunicationUtils.dispatchMessageToNode(joiningNode, message, null);
            }
        }
    }

    public void transferMyKeysToCurrentResponsibleNodes() {
        transferMyKeysToNodes(clusterMap.getNodes());
    }

    public void transferMyKeysToNodes(Set<Node> nodes) {
        System.out.println("Transferring my keys to current responsible nodes...");
        List<String> successfulHashes = new ArrayList<>();
        final Map<String, Boolean> mustDeleteHash = new HashMap<>();
        final Map<String, PutRelayMessage> putMessages = new HashMap<>();

        synchronized (this.getStorageService().getHashLocks()) {
            for (String hash : this.getStorageService().getHashes()) {
                List<Node> responsibleNodes = this.getClusterMap().getNodesResponsibleForHash(hash, REPLICATION_FACTOR);
                mustDeleteHash.put(hash, !responsibleNodes.contains(getStorageService().getNode()));

                File file = new File(this.getStorageService().getValueFilePath(hash));
                byte[] bytes;

                try {
                    bytes = Files.readAllBytes(file.toPath());
                } catch (IOException ignored) {
                    continue;
                }

                for (Node node : nodes) {
                    if (node.id().equals(storageService.getNode().id())) {
                        continue;
                    }
                    boolean mustCopyHash = responsibleNodes.contains(node);
                    if (!mustCopyHash) {
                        continue;
                    }
                    System.out.println("Transferring key " + hash + " to node " + node.id());

                    PutRelayMessage putMessage = putMessages.containsKey(node.id()) ?
                            putMessages.get(node.id()) :
                            new PutRelayMessage();
                    putMessage.setTransference(true);
                    boolean full = putMessage.addValue(hash, bytes);

                    if (full) {
                        successfulHashes.addAll(sendPutRelayMessageToNode(node, putMessage));
                        putMessage = new PutRelayMessage();
                        putMessage.setTransference(true);
                    }

                    putMessages.put(node.id(), putMessage);
                }
            }
        }

        for (Map.Entry<String, PutRelayMessage> entry : putMessages.entrySet()) {
            successfulHashes.addAll(
                    sendPutRelayMessageToNode(
                            clusterMap.getNodeFromId(
                                    entry.getKey()
                            ),
                            entry.getValue()
                    )
            );
        }

        for (String hash : successfulHashes) {
            if (mustDeleteHash.getOrDefault(hash, Boolean.FALSE)) {
                this.getStorageService().delete(hash, false);
            }
        }
    }

    public void removeUnavailableNodeById(String id, boolean incrementCounter) {
        Optional<Node> node = this.clusterMap.getNodes().stream().filter(n -> n.id().equals(id)).findFirst();
        if (node.isEmpty()) {
            return;
        }
        removeUnavailableNode(node.get(), incrementCounter);
    }

    public void removeUnavailableNode(Node node, boolean incrementCounter) {
        boolean mustSendToNewSuccessor = clusterMap.getNodeSuccessor(node).equals(storageService.getNode())
                || clusterMap.getNodeSuccessor(storageService.getNode()).equals(node);

        synchronized (this.membershipLog.getMap()) {
            Integer nodeCounter = this.membershipLog.get(node.id());
            if (nodeCounter != null && nodeCounter % 2 == 0 && incrementCounter) {
                this.membershipLog.put(node.id(), nodeCounter + 1);
            }
        }

        this.clusterMap.remove(node);
        System.out.println(node + " is unavailable. Removed from cluster map");

        if (mustSendToNewSuccessor) {
            System.out.println("Updating key responsible nodes...");
            transferMyKeysToCurrentResponsibleNodes();
        }
    }
}
