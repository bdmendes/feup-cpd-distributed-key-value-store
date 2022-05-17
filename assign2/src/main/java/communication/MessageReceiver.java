package communication;

import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageReceiver {
/*    private final int maxTrials = 3;
    private final int blockMiliseconds = 1000;
    private final boolean infinite;*/
    private final ServerSocket serverSocket;

    public MessageReceiver(ServerSocket socket){
        //this.infinite = infinite;
        this.serverSocket = socket;
    }

    public Message receiveMessage() throws IOException {
        Socket clientSocket = serverSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        MessageReader messageReader = new MessageReader();

        while(!messageReader.isComplete()) {
            messageReader.read(in);
        }

        return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
    }
}
