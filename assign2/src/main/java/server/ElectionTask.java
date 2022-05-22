package server;

import message.ElectionMessage;
import message.Message;

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

        ElectionMessage message = new ElectionMessage();
        message.setOrigin(currentNode.id());
        message.setMembershipLog(membershipService.getMembershipLog().getMap());

        System.out.println("Next node " + nextNode);

        if (nextNode == null) {
            membershipService.setLeader();
            return;
        }

        this.membershipService.sendToNextAvailableNode(message);
        System.out.println("Sent election choose event");
    }
}
