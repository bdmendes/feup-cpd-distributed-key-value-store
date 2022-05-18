package communication;

import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class MessageReceiver {
    private final int blockMiliseconds;
    private final ServerSocket serverSocket;

    public MessageReceiver(ServerSocket socket, int blockMiliseconds){
        this.blockMiliseconds = blockMiliseconds;
        this.serverSocket = socket;
    }

    public Message receiveMessage() throws IOException {
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
}
