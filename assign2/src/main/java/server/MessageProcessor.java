package server;

import message.*;
import message.messagereader.MessageReader;
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

    private void sendMessage(Message message, Socket socket) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(message.encode());
            dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not send message");
        }
    }

    private Message readMessage(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            MessageReader messageReader = new MessageReader();
            while (!messageReader.isComplete()) {
                messageReader.read(bufferedReader);
            }
            return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
        } catch (IOException e) {
            throw new RuntimeException("Could not read message");
        }
    }

    private void dispatchMessageToResponsibleNode(Node responsibleNode, Message message, Socket clientSocket) {
        try (Socket responsibleNodeSocket = new Socket(responsibleNode.id(), responsibleNode.port())){
            this.sendMessage(message, responsibleNodeSocket);
            Message replyMessage = this.readMessage(responsibleNodeSocket);
            if (clientSocket != null) {
                this.sendMessage(replyMessage, clientSocket);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    public void processJoinMessage(JoinMessage joinMessage) {
        if(this.membershipService.getSentMemberships().hasSentMembership(
                joinMessage.getNodeId(),
                joinMessage.getCounter(),
                this.membershipService.getMembershipLog().totalCounter()
        )) {
            System.out.println("Received a duplicate join message");
            return;
        }

        Node newNode = new Node(joinMessage.getNodeId(), joinMessage.getPort());

        this.membershipService.addMembershipEvent(joinMessage.getNodeId(), joinMessage.getCounter());
        this.membershipService.getClusterMap().add(newNode);

        System.out.println(this.membershipService.getClusterMap().getNodes());
        System.out.println(this.membershipService.getMembershipLog(32));

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
            sendMessage(membershipMessage, otherNode);
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

        this.membershipService.addMembershipEvent(leaveMessage.getNodeId(), leaveMessage.getCounter());
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

    private void sendNotJoinedResponse(String requestedKey, Socket clientSocket) {
        GetReply response = new GetReply();
        response.setKey(requestedKey);
        response.setStatusCode(StatusCode.NODE_NOT_JOINED);
        this.sendMessage(response, clientSocket);
    }

    public void processGet(GetMessage getMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendNotJoinedResponse(getMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(getMessage.getKey());
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
            this.sendMessage(response, clientSocket);
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendNotJoinedResponse(putMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(putMessage.getKey());
        if (responsibleNode.equals(this.membershipService.getStorageService().getNode())) {
            PutReply response = new PutReply();
            response.setKey(putMessage.getKey());
            try {
                membershipService.getStorageService().put(putMessage.getKey(), putMessage.getValue());
                response.setStatusCode(StatusCode.OK);
            } catch (IOException e) {
                response.setStatusCode(StatusCode.ERROR);
            }
            this.sendMessage(response, clientSocket);
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket clientSocket) {
        if (!membershipService.isJoined()) {
            this.sendNotJoinedResponse(deleteMessage.getKey(), clientSocket);
            return;
        }

        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(deleteMessage.getKey());
        if (responsibleNode.equals(this.membershipService.getStorageService().getNode())) {
            boolean deleted = membershipService.getStorageService().delete(deleteMessage.getKey());
            DeleteReply response = new DeleteReply();
            response.setKey(deleteMessage.getKey());
            response.setStatusCode(deleted ? StatusCode.OK : StatusCode.FILE_NOT_FOUND);
            this.sendMessage(response, clientSocket);
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket dummy) {
        System.out.println("Received membership message");
        Map<String, Integer> recentLogs = this.membershipService.getMembershipLog(32);

        for (Node node : membershipMessage.getNodes()) {
            boolean loggedRecently = recentLogs.containsKey(node.id());
            if (!loggedRecently) {
                this.membershipService.getClusterMap().add(node);
            }
        }

        for (Map.Entry<String, Integer> entry : membershipMessage.getMembershipLog().entrySet()) {
            String nodeId = entry.getKey();
            Integer membershipCounter = entry.getValue();
            boolean containsEventFromNode = recentLogs.containsKey(nodeId);
            boolean newerThanLocalEvent = containsEventFromNode
                    && recentLogs.get(nodeId) < membershipCounter;
            if (!containsEventFromNode || newerThanLocalEvent) {
                this.membershipService.addMembershipEvent(nodeId, membershipCounter);
                boolean nodeJoined = membershipCounter % 2 == 0;
                if (nodeJoined) {
                    Optional<Node> node = membershipMessage.getNodes().stream().filter(n -> n.id().equals(nodeId)).findFirst();
                    if (node.isEmpty()) continue;
                    this.membershipService.getClusterMap().add(node.get());
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
                this.dispatchMessageToResponsibleNode(joiningNode, putMessage, null);
                this.membershipService.getStorageService().delete(hash);
            }
        }
    }

    @Override
    public void processGetReply(GetReply getReply, Socket socket) {
        sendMessage(getReply, socket);
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) {
        sendMessage(putReply, socket);
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) {
        sendMessage(deleteReply, socket);
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }

}
