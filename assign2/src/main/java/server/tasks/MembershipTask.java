package server.tasks;

import message.MembershipMessage;
import server.MembershipService;

import java.io.IOException;

public class MembershipTask implements Runnable {

    private final MembershipService membershipService;

    public MembershipTask(MembershipService membershipService) {
        this.membershipService = membershipService;
    }


    @Override
    public void run() {
        if (!membershipService.isJoined() || !membershipService.isLeader()) {
            return;
        }

        MembershipMessage membershipMessage = new MembershipMessage();
        membershipMessage.setMembershipLog(this.membershipService.getMembershipLog(32));
        membershipMessage.setNodes(this.membershipService.getClusterMap().getNodes());
        membershipMessage.setNodeId(this.membershipService.getStorageService().getNode().id());

        try {
            membershipService.getMulticastHandler().sendMessage(membershipMessage);
        } catch (IOException e) {
            System.out.println("Could not send multicast from leader");
        }
    }
}
