package server.state;

import message.DeleteRelayMessage;
import message.PutRelayMessage;
import server.MembershipRMI;
import server.MembershipService;

import java.net.Socket;

public class JoiningNodeState extends InitNodeState {
    public JoiningNodeState(MembershipService membershipService) {
        super(membershipService);
    }

    @Override
    public void processPutRelay(PutRelayMessage putRelayMessage, Socket socket) {
        CommonState.processPutRelay(putRelayMessage, socket, this.membershipService);
    }

    @Override
    public void processDeleteRelay(DeleteRelayMessage deleteRelayMessage, Socket clientSocket) {
        CommonState.processDeleteRelay(deleteRelayMessage, clientSocket, this.membershipService);
    }

    @Override
    public MembershipRMI.Status join() {
        return MembershipRMI.Status.JOIN_IN_PROGRESS;
    }
}
