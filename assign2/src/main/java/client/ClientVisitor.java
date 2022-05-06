package client;

import message.*;

import java.io.IOException;
import java.net.Socket;

public class ClientVisitor implements MessageVisitor {
    @Override
    public void processPut(PutMessage putMessage, Socket socket) {

    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {

    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {

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
        if(getReply.getStatusCode() == StatusCode.OK) {
            System.out.println("GET SUCCESS FOR " + getReply.getKey());
            System.out.println("VALUE:");
            System.out.println(new String(getReply.getValue()));
        } else if (getReply.getStatusCode() == StatusCode.FILE_NOT_FOUND) {
            System.out.println("GET FAILURE: NOT FOUND");
        } else {
            System.out.println("GET FAILURE");
        }
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) throws IOException {
        if(putReply.getStatusCode() == StatusCode.OK) {
            System.out.println("PUT SUCCESS FOR " + putReply.getKey());
        } else {
            System.out.println("PUT FAILURE");
        }
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) throws IOException {
        if(deleteReply.getStatusCode() == StatusCode.OK) {
            System.out.println("DELETE SUCCESS FOR " + deleteReply.getKey());
        } else if (deleteReply.getStatusCode() == StatusCode.FILE_NOT_FOUND) {
            System.out.println("DELETE FAILURE: NOT FOUND");
        } else {
            System.out.println("DELETE FAILURE");
        }
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        System.out.println(message.getClass());
        message.accept(this, socket);
    }
}
