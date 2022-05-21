package server;

import communication.MessageSender;
import message.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

public class MessageProcessor implements Runnable, MessageVisitor {
    private final MembershipService membershipService;
    private final StorageService storageService;
    private final Message message;
    private final Socket socket;

    public MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
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
    public void processMembership(MembershipMessage membershipMessage, Socket socket) {
        // update my membership view using Moodle's merge algorithm (in the forum)
        // deixa a magia acontecer
        //MembershipService.doEverything(true);
    }

    @Override
    public void processJoin(JoinMessage joinMessage, Socket socket) {
        if(joinMessage.getNodeId().equals(membershipService.getStorageService().getNode().id())) {
            System.out.println("I'm the new node");
            return;
        }
        System.out.println("new node: " + joinMessage.getNodeId());

        try (Socket otherNode = new Socket(InetAddress.getByName(joinMessage.getNodeId()), joinMessage.getPort())) {
            membershipService.getMembershipLog().put(joinMessage.getNodeId(), joinMessage.getCounter());

            Thread.sleep(new Random().nextInt(500));

            MembershipMessage membershipMessage = new MembershipMessage(membershipService.getClusterNodes(), membershipService.getMembershipLog());
            MessageSender messageSender = new MessageSender(otherNode);
            messageSender.sendMessage(membershipMessage);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
