package server;

import message.*;

import java.io.IOException;
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
    public void processPut(PutMessage putMessage) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // - THEN STORE KEY/VALUE PAIR:
        try {
            storageService.put(putMessage.getKey(), putMessage.getValue());
        } catch (IOException e) {
            // TODO: error handling
            throw new RuntimeException("Could not put key/value pair");
        }
    }

    @Override
    public void processGet(GetMessage getMessage) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // - THEN GET KEY/VALUE PAIR:
        try {
            byte[] value = storageService.get(getMessage.getKey());

            // TODO: use remote object to send value to client
            // for now, just print it to console
            System.out.println(new String(value, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // TODO: error handling
            throw new RuntimeException("Could not get key/value pair");
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage) {
        // FIND NODE TO DELETE KEY/VALUE PAIR

        // - THEN DELETE KEY/VALUE PAIR:
        boolean deleted = storageService.delete(deleteMessage.getKey());

        if(!deleted) {
            // TODO: error handling
            throw new RuntimeException("Could not delete key/value pair");
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage) {

    }

    @Override
    public void processJoin(JoinMessage joinMessage) {

    }

    @Override
    public void processLeave(LeaveMessage leaveMessage) {

    }

    @Override
    public void process(Message message) {
        message.accept(this);
    }
}