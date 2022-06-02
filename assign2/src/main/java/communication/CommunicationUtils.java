package communication;

import message.KeyMessage;
import message.Message;
import message.MessageFactory;
import message.StatusCode;
import message.messagereader.MessageReader;
import server.Node;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class CommunicationUtils {
    private static final int MAX_TRIES = 3;
    private static final int DELAY_MS = 200;

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

    public static Message dispatchMessageToNode(Node node, Message message, Socket clientSocket) {
        for (int i = 0; i < MAX_TRIES; i++) {
            try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())) {
                sendMessage(message, responsibleNodeSocket);
                Message replyMessage = readMessage(responsibleNodeSocket);
                if (clientSocket != null) {
                    System.out.println("Sending dispatched request back to the client");
                    sendMessage(replyMessage, clientSocket);
                }
                return replyMessage;
            } catch (IOException | RuntimeException ignored) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public static boolean dispatchMessageToNodeWithoutReply(Node node, Message message) {
        for (int i = 0; i < MAX_TRIES; i++) {
            try (Socket responsibleNodeSocket = new Socket(node.id(), node.port())) {
                sendMessage(message, responsibleNodeSocket);
                return true;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return false;
    }

    public static void sendErrorResponse(KeyMessage response, StatusCode statusCode, String requestedKey, Socket clientSocket) {
        response.setKey(requestedKey);
        response.setStatusCode(statusCode);
        CommunicationUtils.sendMessage(response, clientSocket);
    }
}
