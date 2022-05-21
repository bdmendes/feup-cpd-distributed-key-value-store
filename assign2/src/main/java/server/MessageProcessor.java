package server;

import communication.MessageSender;
import message.*;
import message.messagereader.MessageReader;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;

public class MessageProcessor implements Runnable, MessageVisitor {
    private final MembershipService membershipService;
    private final StorageService storageService;
    private final Message message;
    private final Socket clientSocket;

    MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
        this.message = message;
        this.storageService = membershipService.getStorageService();
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

    private void sendMessage(Message message, Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.write(message.encode());
        dataOutputStream.flush();
    }

    private Message readMessage(Socket socket) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        MessageReader messageReader = new MessageReader();
        while (!messageReader.isComplete()) {
            messageReader.read(bufferedReader);
        }
        return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
    }

    private void dispatchMessageToResponsibleNode(Node responsibleNode, Message message, Socket clientSocket) {
        try (Socket responsibleNodeSocket = new Socket(responsibleNode.id(), responsibleNode.port())){
            this.sendMessage(message, responsibleNodeSocket);
            Message replyMessage = this.readMessage(responsibleNodeSocket);
            this.sendMessage(replyMessage, clientSocket);
        } catch (IOException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(putMessage.getKey());
        if (responsibleNode.equals(this.storageService.getNode())) {
            PutReply response = new PutReply();
            response.setKey(putMessage.getKey());
            try {
                storageService.put(putMessage.getKey(), putMessage.getValue());
                response.setStatusCode(StatusCode.OK);
            } catch (IOException e) {
                response.setStatusCode(StatusCode.ERROR);
            }
            try {
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(response.encode());
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException("Could not send put reply");
            }
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(getMessage.getKey());
        if (responsibleNode.equals(this.storageService.getNode())) {
            GetReply response = new GetReply();
            response.setKey(getMessage.getKey());
            try {
                byte[] value = storageService.get(getMessage.getKey());
                response.setValue(value);
                response.setStatusCode(StatusCode.OK);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(response.encode());
            } catch (IOException e) {
                response.setStatusCode(StatusCode.FILE_NOT_FOUND);
            }
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(response.encode());
            } catch (IOException e) {
                throw new RuntimeException("Could not send get reply");
            }
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        Node responsibleNode = this.membershipService.getClusterMap().getNodeResponsibleForHash(deleteMessage.getKey());
        if (responsibleNode.equals(this.storageService.getNode())) {
            boolean deleted = storageService.delete(deleteMessage.getKey());
            DeleteReply response = new DeleteReply();
            response.setKey(deleteMessage.getKey());
            if (!deleted) {
                response.setStatusCode(StatusCode.FILE_NOT_FOUND);
            } else {
                response.setStatusCode(StatusCode.OK);
            }
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(response.encode());
            } catch (IOException e) {
                throw new RuntimeException("Could not send delete reply");
            }
        } else {
            this.dispatchMessageToResponsibleNode(responsibleNode, message, clientSocket);
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket dummy) {
        for (Node node : membershipMessage.getNodes()) {
            boolean loggedRecently = this.membershipService.getMembershipLog().containsKey(node.id());
            if (!loggedRecently) {
                this.membershipService.getClusterMap().add(node);
            }
        }

        for (Map.Entry<String, Integer> entry : membershipMessage.getMembershipLog().entrySet()) {
            String nodeId = entry.getKey();
            Integer membershipCounter = entry.getValue();
            boolean containsEventFromNode = this.membershipService.getMembershipLog().containsKey(nodeId);
            boolean newerThanLocalEvent = containsEventFromNode
                    && this.membershipService.getMembershipLog().get(nodeId) < membershipCounter;
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

    @Override
    public void processJoin(JoinMessage joinMessage, Socket socket) {
        // TRANSFER KEYS IF I AM THE SUCCESSOR OF THIS NODE

        membershipService.getMembershipLog().put(joinMessage.getNodeId(), joinMessage.getCounter());
        MembershipMessage membershipMessage = new MembershipMessage(membershipService.getClusterMap().getNodes(), membershipService.getMembershipLog());
        MessageSender messageSender = new MessageSender(socket);
        try {
            messageSender.sendMessage(membershipMessage);
        } catch (IOException ignored) {}
    }

    @Override
    public void processGetReply(GetReply getReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(getReply.encode());
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(putReply.encode());
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(deleteReply.encode());
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }

}
