package server.tasks;

import message.ElectionMessage;
import message.Message;
import server.MembershipService;
import server.Node;

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
        if(!membershipService.isJoined()) {
            return;
        }
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
