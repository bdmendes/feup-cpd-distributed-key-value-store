package server.state;

import message.Message;
import message.MessageVisitor;
import server.MembershipRMI;
import server.MembershipService;
import server.StorageService;

import java.net.Socket;

public abstract class NodeState implements MessageVisitor {
    protected final MembershipService membershipService;
    protected final StorageService storageService;

    public NodeState(MembershipService membershipService) {
        this.membershipService = membershipService;
        this.storageService = membershipService.getStorageService();
    }

    @Override
    public void process(Message message, Socket socket) {
        message.accept(this, socket);
    }

    public abstract MembershipRMI.Status join();

    public abstract MembershipRMI.Status leave();

    public abstract boolean joined();
}
