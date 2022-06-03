package server;

import message.Message;

import java.net.Socket;

public class MessageProcessor implements Runnable {
    private final MembershipService membershipService;
    private final Message message;
    private final Socket clientSocket;

    public MessageProcessor(MembershipService membershipService, Message message, Socket socket) {
        this.message = message;
        this.membershipService = membershipService;
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        this.membershipService.getNodeState().process(this.message, this.clientSocket);
    }
}
