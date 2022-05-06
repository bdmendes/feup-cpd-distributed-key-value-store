package server;

import message.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MembershipService implements MessageVisitor {
    private final StorageService storageService;
    private final int membershipCounter = 0;

    public MembershipService(StorageService storageService) {
        this.storageService = storageService;
    }

    private void sendMembershipStatus() {
        //
    }

    private void joinCluster() {

    }

    private void leaveCluster() {

    }

    public int getMembershipCounter() {
        return membershipCounter;
    }

    @Override
    public void processPut(PutMessage putMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN STORE KEY/VALUE PAIR:
        try {
            storageService.put(putMessage.getKey(), putMessage.getValue());

            PutReply response = new PutReply();
            response.setStatusCode(StatusCode.OK);
            response.setKey(putMessage.getKey());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            // TODO: error handling
            throw new RuntimeException("Could not put key/value pair");
        }
    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN GET KEY/VALUE PAIR:
        try {
            byte[] value = storageService.get(getMessage.getKey());

            GetReply response = new GetReply();
            response.setValue(value);
            response.setStatusCode(StatusCode.OK);
            response.setKey(getMessage.getKey());

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            // TODO: error handling
            throw new RuntimeException("Could not get key/value pair");
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        // FIND NODE TO DELETE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN DELETE KEY/VALUE PAIR:
        boolean deleted = storageService.delete(deleteMessage.getKey());

        if(!deleted) {
            // TODO: error handling
            throw new RuntimeException("Could not delete key/value pair");
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket socket) {

    }

    @Override
    public void processJoin(JoinMessage joinMessage, Socket socket) {

    }

    @Override
    public void processLeave(LeaveMessage leaveMessage, Socket socket) {

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
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }
}