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

        if(!deleted) {
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