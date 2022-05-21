package communication;

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

public class MessageReceiver implements Runnable {
    private final int blockMiliseconds;
    private final ServerSocket serverSocket;
    private final MembershipService membershipService;
    private boolean running;

    public MessageReceiver(MembershipService service, ServerSocket socket, int blockMiliseconds){
        this.membershipService = service;
        this.blockMiliseconds = blockMiliseconds;
        this.serverSocket = socket;
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
        try {
            Message message = receiveMessage();
            if (message != null) {
                MessageProcessor processor = new MessageProcessor(membershipService, message, null);
                processor.run();
            }

            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
