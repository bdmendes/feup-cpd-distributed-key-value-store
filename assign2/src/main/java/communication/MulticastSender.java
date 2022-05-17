package communication;

import message.Message;

import java.io.IOException;
import java.net.*;

public class MulticastSender {
    private final MulticastSocket socket;
    private final Message message;

    public MulticastSender(Message message, IPAddress multicastAddress) throws IOException {
        this.message = message;
        socket = new MulticastSocket(multicastAddress.getPort());
        InetSocketAddress bindAddress = new InetSocketAddress(multicastAddress.getIp(), multicastAddress.getPort());
        socket.joinGroup(bindAddress, null);
    }

    private void sendMessage() {
        byte[] bytes = this.message.encode();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
