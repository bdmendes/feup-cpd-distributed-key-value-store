package server.state;

import message.PutMessage;
import message.PutRelayMessage;
import server.MembershipService;

import java.net.Socket;

public class JoiningNodeState extends InitNodeState {
    public JoiningNodeState(MembershipService membershipService) {
        super(membershipService);
    }

    @Override
    public void processPutRelay(PutRelayMessage putRelayMessage, Socket socket) {
        PutMessage putMessage = new PutMessage();
        putMessage.setKey(putRelayMessage.getKey());
        putMessage.setValue(putRelayMessage.getValue());
        CommonState.processLocalPut(new PutMessage(), socket, this.membershipService);
    }

    @Override
    public boolean join() {
        return true; // TODO: return IN_PROGRESS
    }
}

