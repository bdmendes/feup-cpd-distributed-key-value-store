package server;

import communication.CommunicationUtils;
import message.*;
import utils.MembershipLog;
import utils.StoreUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public class MessageProcessor implements Runnable, MessageVisitor {
    private final MembershipService membershipService;
    private final Message message;
    private final Socket clientSocket;

    public MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
        this.message = message;
        this.membershipService = membershipService;
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            process(this.message, this.clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processJoinMessage(JoinMessage joinMessage) {
        if (this.membershipService.getSentMemberships().hasSentMembership(
                joinMessage.getNodeId(),
                joinMessage.getCounter(),
                this.membershipService.getMembershipLog().totalCounter()
        )) {
            System.out.println("Received a duplicate join message");
            return;
        }

        Node newNode = new Node(joinMessage.getNodeId(), joinMessage.getPort());

        this.membershipService.getMembershipLog().put(joinMessage.getNodeId(), joinMessage.getCounter());
        this.membershipService.getClusterMap().put(newNode);

        try (Socket otherNode = new Socket(InetAddress.getByName(joinMessage.getNodeId()), joinMessage.getConnectionPort())) {
            Thread.sleep(new Random().nextInt(1200));
            if(this.membershipService.getSentMemberships().hasSentMembership(
                    joinMessage.getNodeId(),
                    joinMessage.getCounter(),
                    this.membershipService.getMembershipLog().totalCounter()
            )) {
                System.out.println("Received a duplicate join message");
                return;
            }

            MembershipMessage membershipMessage = new MembershipMessage();

            membershipMessage.setMembershipLog(membershipService.getMembershipLog(32));
            membershipMessage.setNodes(membershipService.getClusterMap().getNodes());
            membershipMessage.setNodeId(membershipService.getStorageService().getNode().id());
            CommunicationUtils.sendMessage(membershipMessage, otherNode);
            this.membershipService.getSentMemberships().saveSentMembership(
                    joinMessage.getNodeId(),
                    joinMessage.getCounter(),
                    this.membershipService.getMembershipLog().totalCounter()
            );
            System.out.println("sent membership message to " + joinMessage.getNodeId());


            if (membershipService.getClusterMap().getNodeSuccessorById(joinMessage.getNodeId())
                    .equals(membershipService.getStorageService().getNode())){
                this.transferKeysToJoiningNode(newNode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void processLeaveMessage(JoinMessage leaveMessage) {
        System.out.println("left node: " + leaveMessage.getNodeId());

        this.membershipService.getMembershipLog().put(leaveMessage.getNodeId(), leaveMessage.getCounter());
        this.membershipService.getClusterMap().remove(new Node(leaveMessage.getNodeId(), leaveMessage.getPort()));

        System.out.println(this.membershipService.getClusterMap().getNodes());
        System.out.println(this.membershipService.getMembershipLog(32));
    }

    @Override
    public void processJoin(JoinMessage joinMessage, Socket dummy) {
        if (joinMessage.getNodeId().equals(membershipService.getStorageService().getNode().id())) {
            return;
        }

        if (joinMessage.getCounter() % 2 == 0) {
            processJoinMessage(joinMessage);
        } else {
            processLeaveMessage(joinMessage);
        }
    }

    private void sendErrorResponse(ReplyKeyMessage response, StatusCode statusCode, String requestedKey, Socket clientSocket) {
        response.setKey(requestedKey);
        response.setStatusCode(statusCode);
        CommunicationUtils.sendMessage(response, clientSocket);
    }

    public void processGet(GetMessage getMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendErrorResponse(new GetReply(), StatusCode.NODE_NOT_JOINED, getMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(getMessage.getKey());
        if (responsibleNode == null) {
            this.sendErrorResponse(new GetReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, getMessage.getKey(), clientSocket);
            return;
        }

        if (responsibleNode.equals(this.membershipService.getStorageService().getNode())) {
            GetReply response = new GetReply();
            response.setKey(getMessage.getKey());
            try {
                byte[] value = membershipService.getStorageService().get(getMessage.getKey());
                response.setValue(value);
                response.setStatusCode(StatusCode.OK);
            } catch (IOException e) {
                response.setStatusCode(StatusCode.FILE_NOT_FOUND);
            }
            System.out.println("Getting hash " + getMessage.getKey());
            CommunicationUtils.sendMessage(response, clientSocket);
        } else {
            System.out.println("Dispatching get request for hash " + getMessage.getKey() + " to node " + responsibleNode);
            CommunicationUtils.dispatchMessageToNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendErrorResponse(new PutReply(), StatusCode.NODE_NOT_JOINED, putMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(putMessage.getKey());
        if (responsibleNode == null) {
            this.sendErrorResponse(new PutReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, putMessage.getKey(), clientSocket);
            return;
        }

        if (responsibleNode.equals(this.membershipService.getStorageService().getNode())) {
            PutReply response = new PutReply();
            response.setKey(putMessage.getKey());
            try {
                membershipService.getStorageService().put(putMessage.getKey(), putMessage.getValue());
                response.setStatusCode(StatusCode.OK);
            } catch (IOException e) {
                response.setStatusCode(StatusCode.ERROR);
            }
            System.out.println("Putting hash " + putMessage.getKey());
            CommunicationUtils.sendMessage(response, clientSocket);
        } else {
            System.out.println("Dispatching put request for hash " + putMessage.getKey() + " to node " + responsibleNode);
            CommunicationUtils.dispatchMessageToNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendErrorResponse(new DeleteReply(), StatusCode.NODE_NOT_JOINED, deleteMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(deleteMessage.getKey());
        if (responsibleNode == null) {
            this.sendErrorResponse(new DeleteReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, deleteMessage.getKey(), clientSocket);
            return;
        }

        if (responsibleNode.equals(this.membershipService.getStorageService().getNode())) {
            boolean deleted = membershipService.getStorageService().delete(deleteMessage.getKey());
            DeleteReply response = new DeleteReply();
            response.setKey(deleteMessage.getKey());
            response.setStatusCode(deleted ? StatusCode.OK : StatusCode.FILE_NOT_FOUND);
            System.out.println("Deleting hash " + deleteMessage.getKey());
            CommunicationUtils.sendMessage(response, clientSocket);
        } else {
            System.out.println("Dispatching delete request for hash " + deleteMessage.getKey() + " to node " + responsibleNode);
            CommunicationUtils.dispatchMessageToNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket dummy) {
        System.out.println("Received membership message");
        Map<String, Integer> recentLogs = this.membershipService.getMembershipLog(32);

        for (Node node : membershipMessage.getNodes()) {
            boolean loggedRecently = recentLogs.containsKey(node.id());
            if (!loggedRecently) {
                this.membershipService.getClusterMap().put(node);
            }
        }

        for (Map.Entry<String, Integer> entry : membershipMessage.getMembershipLog().entrySet()) {
            String nodeId = entry.getKey();
            Integer membershipCounter = entry.getValue();
            boolean containsEventFromNode = recentLogs.containsKey(nodeId);
            boolean newerThanLocalEvent = containsEventFromNode
                    && recentLogs.get(nodeId) < membershipCounter;
            if (!containsEventFromNode || newerThanLocalEvent) {
                this.membershipService.getMembershipLog().put(nodeId, membershipCounter);
                boolean nodeJoined = membershipCounter % 2 == 0;
                if (nodeJoined) {
                    Optional<Node> node = membershipMessage.getNodes().stream().filter(n -> n.id().equals(nodeId)).findFirst();
                    if (node.isEmpty()) continue;
                    this.membershipService.getClusterMap().put(node.get());
                } else {
                    this.membershipService.getClusterMap().removeId(nodeId);
                }
            }
        }
    }

    private void transferKeysToJoiningNode(Node joiningNode) {
        String joiningNodeHash = StoreUtils.sha256(joiningNode.id().getBytes(StandardCharsets.UTF_8));
        for (String hash : membershipService.getStorageService().getHashes()) {
            if (joiningNodeHash.compareTo(hash) > 0) {
                PutMessage putMessage = new PutMessage();
                try {
                    File file = new File(membershipService.getStorageService().getValueFilePath(hash));
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String key = StoreUtils.sha256(bytes);
                    putMessage.setKey(key);
                    putMessage.setValue(bytes);
                } catch (IOException e) {
                    throw new IllegalArgumentException("File not found");
                }
                CommunicationUtils.dispatchMessageToNodeWithoutReply(joiningNode, putMessage);
                this.membershipService.getStorageService().delete(hash);
            }
        }
    }

    @Override
    public void processGetReply(GetReply getReply, Socket socket) {
        CommunicationUtils.sendMessage(getReply, socket);
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) {
        CommunicationUtils.sendMessage(putReply, socket);
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) {
        CommunicationUtils.sendMessage(deleteReply, socket);
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }

    @Override
    public void processElection(ElectionMessage electionMessage, Socket socket) {
        Map<String, Integer> incomingMembershipLog = electionMessage.getMembershipLog();
        String origin = electionMessage.getOrigin();

        Node currentNode = membershipService.getStorageService().getNode();
        Node nextNode = membershipService.getClusterMap().getNodeSuccessor(currentNode);

        System.out.println("Origin: " + nextNode + " " + origin + " " + currentNode.id());
        if (origin.equals(currentNode.id())) {
            membershipService.setLeader();
            LeaderMessage message = new LeaderMessage();
            message.setLeaderNode(origin);
            System.out.println("here " + message + " " + nextNode);
            this.membershipService.sendToNextAvailableNode(message);
            return;
        }

        MembershipLog membershipLog = membershipService.getMembershipLog();

        Integer membershipDifference = incomingMembershipLog.entrySet()
                .parallelStream()
                .reduce(0, (Integer subtotal, Map.Entry<String, Integer> element) -> {
                    Integer current = membershipLog.get(element.getKey());

                    if (current == null) {
                        return subtotal - element.getValue();
                    }

                    return subtotal + current - element.getValue();
                }, Integer::sum);

        if (membershipDifference <= 0 && currentNode.id().compareTo(origin) <= 0) {
            return;
        }

        this.membershipService.sendToNextAvailableNode(electionMessage);
    }

    @Override
    public void processLeader(LeaderMessage leaderMessage, Socket socket) {
        if (leaderMessage.getLeaderNode().equals(membershipService.getStorageService().getNode().id())) {
            System.out.println(membershipService.getStorageService().getNode().id() + " is leader.");
            return;
        }

        membershipService.unsetLeader();
        System.out.println(membershipService.getStorageService().getNode().id() + " is not leader.");

        Node currentNode = membershipService.getStorageService().getNode();
        Node nextNode = membershipService.getClusterMap().getNodeSuccessor(currentNode);


        this.membershipService.sendToNextAvailableNode(leaderMessage);
    }

}
