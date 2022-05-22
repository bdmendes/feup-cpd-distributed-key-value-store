package server;

import message.ElectionMessage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ElectionTask implements Runnable {

    private final MembershipService membershipService;

    public ElectionTask(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public void run() {
        Node currentNode = membershipService.getStorageService().getNode();
        Node nextNode = membershipService.getClusterMap().getNodeSuccessor(currentNode);

        System.out.println("wtf");

        ElectionMessage message = new ElectionMessage();
        message.setOrigin(currentNode.id());
        message.setMembershipLog(membershipService.getMembershipLog());

        if (nextNode == null) {
            membershipService.setLeader();
            return;
        }

        try {
            System.out.println(nextNode);
            Socket socket = new Socket(nextNode.id(), nextNode.port());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(message.encode());
            dataOutputStream.flush();
            socket.close();
            System.out.println("Sent election choose event");
        } catch (IOException e) {
            // throw new RuntimeException("Could not send message");
        }
    }
}
