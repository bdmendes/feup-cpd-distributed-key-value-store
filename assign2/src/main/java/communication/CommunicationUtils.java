package communication;

import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;
import server.Node;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class CommunicationUtils {
    public static void sendMessage(Message message, Socket socket) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.write(message.encode());
            dataOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not send message");
        }
    }

    public static Message readMessage(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            MessageReader messageReader = new MessageReader();
            while (!messageReader.isComplete()) {
                messageReader.read(bufferedReader);
            }
            return MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
        } catch (IOException e) {
            throw new RuntimeException("Could not read message");
        }
    }

    public static void dispatchMessageToNode(Node node, Message message, Socket clientSocket) {
        try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())){
            sendMessage(message, responsibleNodeSocket);
            Message replyMessage = readMessage(responsibleNodeSocket);
            if (clientSocket != null) {
                System.out.println("Sending dispatched request back to the client");
                sendMessage(replyMessage, clientSocket);
            }
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    public static Message dispatchMessageToNodeWithReply(Node node, Message message) {
        try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())){
            sendMessage(message, responsibleNodeSocket);
            Message replyMessage = readMessage(responsibleNodeSocket);
            return replyMessage;
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }

    public static void dispatchMessageToNodeWithoutReply(Node node, Message message) {
        try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())){
            sendMessage(message, responsibleNodeSocket);
        } catch (IOException e) {
            throw new RuntimeException("Could not request operation to responsible node");
        }
    }
}
