package server.state;

import communication.CommunicationUtils;
import message.*;
import server.MembershipService;
import server.Node;
import utils.MembershipLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class JoinedNodeState extends NodeState {
    public JoinedNodeState(MembershipService membershipService) {
        super(membershipService);

        Thread multicastHandlerThread = new Thread(this.membershipService.getMulticastHandler());
        multicastHandlerThread.start();
    }

    private void sendNodeDownMessage(ReplyKeyMessage response, String key, Socket clientSocket) {
        response.setKey(key);
        response.setStatusCode(StatusCode.NODE_IS_DOWN);
        CommunicationUtils.sendMessage(response, clientSocket);
    }

    private void replyValueFromStore(Socket socket, String key) {
        GetReply response = new GetReply();
        response.setKey(key);
        try {
            byte[] value;
            synchronized (storageService.getHashLock(key)) {
                value = membershipService.getStorageService().get(key);
            }
            response.setValue(value);
            response.setStatusCode(StatusCode.OK);
            System.out.println("Getting hash " + key);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            response.setStatusCode(StatusCode.FILE_NOT_FOUND);
        }
        CommunicationUtils.sendMessage(response, socket);
    }

    @Override
    public void processGetRelay(GetRelayMessage getRelayMessage, Socket clientSocket) {
        replyValueFromStore(clientSocket, getRelayMessage.getKey());
    }

    @Override
    public void processGet(GetMessage getMessage, Socket clientSocket) {
        List<Node> responsibleNodes = this.membershipService.getClusterMap()
                .getNodesResponsibleForHash(getMessage.getKey(), MembershipService.REPLICATION_FACTOR);

        for (Node node : responsibleNodes) {
            if (node.id().equals(this.storageService.getNode().id())
                    && this.storageService.getHashes().contains(getMessage.getKey())) {
                replyValueFromStore(clientSocket, getMessage.getKey());
                return;
            } else {
                GetRelayMessage getRelayMessage = new GetRelayMessage();
                getRelayMessage.setKey(getMessage.getKey());
                getRelayMessage.setTarget(node.id());
                try {
                    GetReply message = (GetReply) CommunicationUtils.dispatchMessageToNode(node, getRelayMessage, null);
                    if (message == null) {
                        this.membershipService.removeUnavailableNode(node);
                        processGet(getMessage, clientSocket);
                        continue;
                    }
                    if (!message.getStatusCode().equals(StatusCode.OK)) {
                        continue;
                    }
                    System.out.println("Successfully GET from replicated node " + node);
                    CommunicationUtils.sendMessage(message, clientSocket);
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        CommunicationUtils.sendErrorResponse(new GetReply(), StatusCode.FILE_NOT_FOUND, getMessage.getKey(), clientSocket);
    }

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
        List<Node> responsibleNodes = this.membershipService.getClusterMap()
                .getNodesResponsibleForHash(putMessage.getKey(), MembershipService.REPLICATION_FACTOR);

        PutRelayMessage putRelayMessage = new PutRelayMessage();
        putRelayMessage.addValue(putMessage.getKey(), putMessage.getValue());

        PutReply response = new PutReply();
        response.setKey(putMessage.getKey());

        boolean positiveResponse = false;
        for (Node node : responsibleNodes) {
            if (node.id().equals(this.storageService.getNode().id())) {
                try {
                    membershipService.getStorageService().put(putMessage.getKey(), putMessage.getValue());
                    response.setStatusCode(StatusCode.OK);
                } catch (IOException e) {
                    response.setStatusCode(StatusCode.ERROR);
                }
                System.out.println("Putting hash " + putMessage.getKey());
                if (!positiveResponse && response.getStatusCode() == StatusCode.OK) {
                    positiveResponse = true;
                }
            } else {
                System.out.println("Dispatching put replication request for hash " + putMessage.getKey() + " to " + node);
                PutRelayReply putRelayReply = (PutRelayReply) CommunicationUtils.dispatchMessageToNode(node, putRelayMessage, null);
                if (putRelayReply == null) {
                    this.membershipService.removeUnavailableNode(node);
                    processPut(putMessage, clientSocket);
                    return;
                }
                if (!putRelayReply.getStatusCode().equals(StatusCode.OK)) {
                    continue;
                }
                if (!positiveResponse) {
                    response.setStatusCode(putRelayReply.getStatusCode());
                    positiveResponse = true;
                }
            }
        }

        if (!positiveResponse) {
            sendNodeDownMessage(new PutReply(), putMessage.getKey(), clientSocket);
        } else {
            CommunicationUtils.sendMessage(response, clientSocket);
        }
    }

    @Override
    public void processPutRelay(PutRelayMessage putRelayMessage, Socket socket) {
        CommonState.processPutRelay(putRelayMessage, socket, this.membershipService);
    }

    @Override
    public void processDeleteRelay(DeleteRelayMessage deleteRelayMessage, Socket clientSocket) {
        boolean deleted;
        synchronized (storageService.getHashLock(deleteRelayMessage.getKey())) {
            deleted = membershipService.getStorageService().delete(deleteRelayMessage.getKey());
        }
        System.out.println("Deleting hash " + deleteRelayMessage.getKey());
        DeleteRelayReply response = new DeleteRelayReply();
        response.reportSuccess(deleteRelayMessage.getKey());
        response.setStatusCode(deleted ? StatusCode.OK : StatusCode.FILE_NOT_FOUND);
        CommunicationUtils.sendMessage(response, clientSocket);
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket clientSocket) {
        List<Node> responsibleNodes = this.membershipService.getClusterMap()
                .getNodesResponsibleForHash(deleteMessage.getKey(), MembershipService.REPLICATION_FACTOR);

        DeleteRelayMessage deleteRelayMessage = new DeleteRelayMessage();
        deleteRelayMessage.setKey(deleteMessage.getKey());

        DeleteReply response = new DeleteReply();
        response.setKey(deleteMessage.getKey());

        boolean positiveResponse = false;
        for (Node node : responsibleNodes) {
            if (node.id().equals(this.storageService.getNode().id())) {
                boolean deleted;
                synchronized (storageService.getHashLock(deleteMessage.getKey())) {
                    deleted = membershipService.getStorageService().delete(deleteMessage.getKey());
                }
                System.out.println("Deleting hash " + deleteMessage.getKey());
                if (deleted) {
                    response.setStatusCode(StatusCode.OK);
                }
                if (!positiveResponse && response.getStatusCode() == StatusCode.OK) {
                    positiveResponse = true;
                }
            } else {
                System.out.println("Dispatching delete request for hash " + deleteMessage.getKey() + " to " + node);
                DeleteRelayReply deleteRelayReply = (DeleteRelayReply)
                        CommunicationUtils.dispatchMessageToNode(node, deleteRelayMessage, null);
                if (deleteRelayReply == null) {
                    this.membershipService.removeUnavailableNode(node);
                    return;
                }
                if (!positiveResponse && deleteRelayReply.getStatusCode() == StatusCode.OK) {
                    response.setStatusCode(deleteRelayReply.getStatusCode());
                    positiveResponse = true;
                }
            }
        }

        if (!positiveResponse) {
            sendNodeDownMessage(new DeleteReply(), deleteMessage.getKey(), clientSocket);
        } else {
            CommunicationUtils.sendMessage(response, clientSocket);
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket socket) {
        CommonState.processMembership(membershipMessage, this.membershipService);
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
            if (this.membershipService.getSentMemberships().hasSentMembership(
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
            this.membershipService.transferKeysToJoiningNode(newNode);
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
    public void processElection(ElectionMessage electionMessage, Socket socket) {
        Map<String, Integer> incomingMembershipLog = electionMessage.getMembershipLog();
        String origin = electionMessage.getOrigin();

        Node currentNode = membershipService.getStorageService().getNode();
        Node nextNode = membershipService.getClusterMap().getNodeSuccessor(currentNode);
        System.out.println("Received election message from: " + origin + "; dispatching to next node: " + nextNode);
        if (origin.equals(currentNode.id())) {
            membershipService.setLeader(true);
            LeaderMessage message = new LeaderMessage();
            message.setLeaderNode(origin);
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
        System.out.println(membershipService.getStorageService().getNode().id() + " is not leader.");

        membershipService.setLeader(false);
        this.membershipService.sendToNextAvailableNode(leaderMessage);
    }

    @Override
    public boolean join() {
        return true;
    }

    @Override
    public boolean leave() {
        synchronized (this.membershipService.joinLeaveLock) {
            if (!this.membershipService.isJoined()) {
                return true;
            }

            this.membershipService.setNodeState(new InitNodeState(this.membershipService));
            this.membershipService.getMessageReceiverTask().waitAndRestart();
            try {
                this.membershipService.getMulticastHandler().waitTasks();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                this.membershipService.getMembershipCounter().incrementAndGet();
                JoinMessage message = this.membershipService.createJoinMessage(-1);

                this.membershipService.getMulticastHandler().sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                this.membershipService.setNodeState(this);
                return false;
            }

            try {
                this.membershipService.getMulticastHandler().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.membershipService.transferAllMyKeysToNewSuccessors();
            this.membershipService.setLeader(false);
            this.membershipService.getClusterMap().clear();
            this.membershipService.getMembershipLog().clear();

            System.out.println("Left cluster");
        }

        return true;
    }

    @Override
    public boolean joined() {
        return true;
    }
}
