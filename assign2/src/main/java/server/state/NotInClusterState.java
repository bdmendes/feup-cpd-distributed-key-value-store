package server.state;

import server.MembershipService;

public abstract class NotInClusterState extends NodeState{
    public NotInClusterState(MembershipService membershipService) {
        super(membershipService);
    }

    @Override
    public boolean leave() {
        System.out.println("Node is not in a cluster.");
        return false;
    }

    @Override
    public boolean canSendMembership() {
        return false;
    }

    @Override
    public boolean canSendElection() {
        return false;
    }
}
