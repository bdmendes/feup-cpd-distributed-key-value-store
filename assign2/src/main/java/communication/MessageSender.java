package communication;

import message.Message;
import java.io.IOException;
import java.net.Socket;

public class MessageSender {
    private final Socket socket;

    MessageSender(IPAddress ipAddress) throws IOException {
        this.socket = new Socket(ipAddress.getIp(), ipAddress.getPort());
    }

    public void sendMessage(Message message) throws IOException {
        if (socket.isClosed()){
            throw new IOException("Socket has already been closed.");
        }
        socket.getOutputStream().write(message.encode());
        socket.close();
    }
}
