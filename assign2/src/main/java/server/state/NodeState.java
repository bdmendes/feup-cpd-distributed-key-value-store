package server.state;

import message.Message;
import message.MessageVisitor;
import server.MembershipService;
import server.StorageService;

import java.io.IOException;
import java.net.Socket;

public abstract class NodeState implements MessageVisitor {
    protected final MembershipService membershipService;
    protected final StorageService storageService;

    public NodeState(MembershipService membershipService) {
        this.membershipService = membershipService;
        this.storageService = membershipService.getStorageService();
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }

    public abstract boolean join();

    public abstract boolean leave();

    public abstract boolean joined();
}
