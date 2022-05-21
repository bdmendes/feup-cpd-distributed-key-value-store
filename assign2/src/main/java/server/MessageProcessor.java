package server;

import message.*;
import message.messagereader.MessageReader;
import utils.StoreUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public class MessageProcessor implements Runnable, MessageVisitor {
    private final MembershipService membershipService;
    private final Message message;
    private final Socket clientSocket;

    MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
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

    @Override
    public void processPut(PutMessage putMessage, Socket clientSocket) {
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
    public void processGet(GetMessage getMessage, Socket clientSocket) {
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
    public void processDelete(DeleteMessage deleteMessage, Socket clientSocket) {
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
    public void processJoin(JoinMessage joinMessage, Socket clientSocket) {
        membershipService.getMembershipLog().put(joinMessage.getNodeId(), joinMessage.getCounter());
        MembershipMessage membershipMessage = new MembershipMessage(membershipService.getClusterMap().getNodes(),
                membershipService.getMembershipLog());
        sendMessage(membershipMessage, clientSocket);

        if (membershipService.getClusterMap().getNodeSuccessorById(joinMessage.getNodeId())
                .equals(membershipService.getStorageService().getNode())){
            // call transfer keys when join message has the port, to pass a node
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
