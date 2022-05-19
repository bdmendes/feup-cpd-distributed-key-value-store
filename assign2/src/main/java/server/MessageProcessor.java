package server;

import communication.MessageSender;
import message.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;

public class MessageProcessor implements Runnable, MessageVisitor {
    private final MembershipService membershipService;
    private final StorageService storageService;
    private final Message message;
    private final Socket socket;

    MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
        this.message = message;
        this.storageService = membershipService.getStorageService();
        this.membershipService = membershipService;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            process(this.message, this.socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    @Override
    public void processPut(PutMessage putMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN STORE KEY/VALUE PAIR:
        PutReply response = new PutReply();
        response.setKey(putMessage.getKey());

        try {
            storageService.put(putMessage.getKey(), putMessage.getValue());
            response.setStatusCode(StatusCode.OK);
        } catch (IOException e) {
            response.setStatusCode(StatusCode.ERROR);
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            throw new RuntimeException("Could not send put reply");
        }
    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN GET KEY/VALUE PAIR:
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
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        // FIND NODE TO DELETE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN DELETE KEY/VALUE PAIR:
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
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket dummy) {
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
                    this.membershipService.getClusterNodes().add(node.get());
                } else {
                    this.membershipService.getClusterNodes().removeIf(n -> n.id().equals(nodeId));
                }
            }
        }
    }

    @Override
    public void processJoin(JoinMessage joinMessage, Socket socket) {
        membershipService.getMembershipLog().put(joinMessage.getNodeId(), joinMessage.getCounter());
        MembershipMessage membershipMessage = new MembershipMessage(membershipService.getClusterNodes(), membershipService.getMembershipLog());
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
