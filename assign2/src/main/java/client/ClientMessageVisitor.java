package client;

import message.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientMessageVisitor implements MessageVisitor {
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
    public void processGetReply(GetReply getReply, Socket socket) {
        if(getReply.getStatusCode() == StatusCode.OK) {
            System.out.println("GET SUCCESS FOR " + getReply.getKey());
            System.out.println("VALUE:");
            System.out.println(new String(getReply.getValue(), StandardCharsets.UTF_8));
        } else {
            System.out.println("GET FAILURE: " + getReply.getStatusCode());
        }
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) {
        if(putReply.getStatusCode() == StatusCode.OK) {
            System.out.println("PUT SUCCESS FOR " + putReply.getKey());
        } else {
            System.out.println("PUT FAILURE: " + putReply.getStatusCode());
        }
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) {
        if (deleteReply.getStatusCode() == StatusCode.OK) {
            System.out.println("DELETE SUCCESS FOR " + deleteReply.getKey());
        } else {
            System.out.println("DELETE FAILURE: " + deleteReply.getStatusCode());
        }
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        System.out.println(message.getClass());
        message.accept(this, socket);
    }

    @Override
    public void processElection(ElectionMessage electionMessage, Socket socket) {}

    @Override
    public void processLeader(LeaderMessage leaderMessage, Socket socket) {}
}
