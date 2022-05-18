package communication;

import message.Message;

import java.io.IOException;
import java.net.*;

public class MulticastSender {
    private final MulticastSocket socket;
    private final Message message;
    private final InetSocketAddress bindAddress;

    public MulticastSender(Message message, IPAddress multicastAddress) throws IOException {
        this.message = message;
        socket = new MulticastSocket(multicastAddress.getPort());
        bindAddress = new InetSocketAddress(multicastAddress.getIp(), multicastAddress.getPort());

        //System.out.println("bind address " + NetworkInterface.getByInetAddress(bindAddress.getAddress()));
        socket.joinGroup(bindAddress, NetworkInterface.getByInetAddress(bindAddress.getAddress()));
    }

    public void sendMessage() {
        byte[] bytes = this.message.encode();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, bindAddress);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
