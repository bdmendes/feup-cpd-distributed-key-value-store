package server.tasks;

import message.ElectionMessage;
import server.MembershipService;
import server.Node;

public class ElectionTask implements Runnable {

    private final MembershipService membershipService;

    public ElectionTask(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public void run() {
        if (!membershipService.isJoined()) {
            return;
        }
        Node currentNode = membershipService.getStorageService().getNode();
        Node nextNode = membershipService.getClusterMap().getNodeSuccessor(currentNode);

        ElectionMessage message = new ElectionMessage();
        message.setOrigin(currentNode.id());
        message.setMembershipLog(membershipService.getMembershipLog().getMap());

        if (nextNode == null) {
            membershipService.setLeader(true);
            return;
        }

        this.membershipService.sendToNextAvailableNode(message);
        System.out.println("Sent election choose event");
    }
}
