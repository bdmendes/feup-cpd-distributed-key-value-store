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
            System.out.println("GET SUCCESS");
            System.out.println("VALUE:");
            System.out.println(new String(getReply.getValue()));
        } else {
            System.out.println("GET FAILURE");
        }
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        System.out.println(message.getClass());
        message.accept(this, socket);
    }
}
