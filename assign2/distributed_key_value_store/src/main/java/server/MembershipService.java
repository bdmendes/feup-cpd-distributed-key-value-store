package server;

import message.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MembershipService implements MessageVisitor {
    private int membershipCounter = 0;
    private final StorageService storageService;

    static String sha256(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] digest = messageDigest.digest();
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : digest) {
                stringBuilder.append(String.format("%02x", b));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            return str;
        }
    }

    public MembershipService(StorageService storageService) {
        this.storageService = storageService;
    }

    private void sendMembershipStatus() {
        //
    }

    private void joinCluster(){

    }

    private void leaveCluster() {

    }

    public int getMembershipCounter() {
        return membershipCounter;
    }

    @Override
    public void processPut(PutMessage putMessage) {

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