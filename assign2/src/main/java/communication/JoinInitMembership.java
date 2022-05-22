package communication;

import message.MembershipMessage;
import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;
import server.MembershipService;
import server.MessageProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

public class JoinInitMembership implements Runnable {
    private final int blockMiliseconds;
    private final ServerSocket serverSocket;
    private final MembershipService membershipService;
    private final MulticastHandler multicastHandler;
    private final Message retransmitMessage;
    private boolean running;

    public JoinInitMembership(MembershipService service, ServerSocket socket, Message retransmit, MulticastHandler handler, int blockMiliseconds){
        this.membershipService = service;
        this.blockMiliseconds = blockMiliseconds;
        this.multicastHandler = handler; // what if handler is closed while this is running?
        this.serverSocket = socket;
        this.retransmitMessage = retransmit;
        this.running = true;
    }

    private Message receiveMessage() throws IOException {
        serverSocket.setSoTimeout(this.blockMiliseconds);
        Socket clientSocket;
        try {
            clientSocket = serverSocket.accept();
        } catch (SocketTimeoutException e){
            return null;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        MessageReader messageReader = new MessageReader();

        while(!messageReader.isComplete()) {
            messageReader.read(in);
        }

        return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
    }

    public void close() throws IOException {
        this.running = false;
        this.serverSocket.close();
    }

    @Override
    public void run() {
        int retransmitted = 1;
        int received = 0;
        Set<String> receivedMessages = new HashSet<>();

        while(retransmitted <= 3 && received < 3 && running) {
            Message receivedMessage;

            try {
                System.out.println("Waiting for membership message");
                receivedMessage = receiveMessage();
            } catch (Exception e) {
                continue;
            }

            if(receivedMessage == null) {
                if(retransmitted == 3) {
                    break;
                }

                try {
                    multicastHandler.sendMessage(retransmitMessage);

                    System.out.println("Retransmitted join message");
                    retransmitted++;
                } catch (IOException e) {
                    return;
                }

                continue;
            }

            MessageProcessor processor = new MessageProcessor(membershipService, receivedMessage, null);
            processor.run();

            if(receivedMessages.contains(((MembershipMessage) receivedMessage).getNodeId())) {
                continue;
            }

            receivedMessages.add(((MembershipMessage) receivedMessage).getNodeId());
            received++;
        }

        System.out.println("Received " + received + " messages");
        System.out.println(this.membershipService.getClusterMap().getNodes());
        System.out.println(this.membershipService.getMembershipLog(32));
    }
}
