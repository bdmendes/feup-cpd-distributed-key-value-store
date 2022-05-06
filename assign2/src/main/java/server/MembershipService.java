package server;

import message.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        try {
            storageService.put(putMessage.getKey(), putMessage.getValue());
        } catch (IOException e) {
            // TODO: error handling
            e.printStackTrace();
        }
    }

    @Override
    public void processGet(GetMessage getMessage) {

    }

    @Override
    public void processDelete(DeleteMessage deleteMessage) {

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