package server.state;

import communication.CommunicationUtils;
import message.*;
import server.MembershipService;
import server.Node;
import utils.MembershipLog;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Random;

public class JoinedNodeState extends NodeState {
    public JoinedNodeState(MembershipService membershipService) {
        super(membershipService);

        Thread multicastHandlerThread = new Thread(this.membershipService.getMulticastHandler());
        multicastHandlerThread.start();
    }

    @Override
    public void processGet(GetMessage getMessage, Socket clientSocket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(getMessage.getKey());
        if (responsibleNode == null) {
            CommunicationUtils.sendErrorResponse(new GetReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, getMessage.getKey(), clientSocket);
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
            CommunicationUtils.dispatchMessageToNode(responsibleNode, getMessage, clientSocket);
        }
    }

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(putMessage.getKey());
        if (responsibleNode == null) {
            CommunicationUtils.sendErrorResponse(new PutReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, putMessage.getKey(), clientSocket);
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
            CommunicationUtils.dispatchMessageToNode(responsibleNode, putMessage, clientSocket);
        }
    }

    @Override
    public void processPutRelay(PutRelayMessage putRelayMessage, Socket socket) {
        CommonState.processPutRelay(putRelayMessage, socket, this.membershipService);
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket clientSocket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(deleteMessage.getKey());
        if (responsibleNode == null) {
            CommunicationUtils.sendErrorResponse(new DeleteReply(), StatusCode.UNKNOWN_CLUSTER_VIEW, deleteMessage.getKey(), clientSocket);
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
            CommunicationUtils.dispatchMessageToNode(responsibleNode, deleteMessage, clientSocket);
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

            if (membershipService.getClusterMap().getNodeSuccessorById(joinMessage.getNodeId())
                    .equals(membershipService.getStorageService().getNode())) {
                this.membershipService.transferKeysToJoiningNode(newNode);
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
            if(!this.membershipService.isJoined()) {
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

            this.membershipService.transferAllMyKeysToMySuccessor();

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
