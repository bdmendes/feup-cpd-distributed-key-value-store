package communication;

import message.Message;
import server.Node;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;

public class MulticastHandler implements Runnable {
    private final MulticastSocket socket;
    private final InetSocketAddress multicastAddress;
    private final NetworkInterface networkInterface;
    private boolean running = true;
    private static final int MAX_BUF_LEN = 2000;

    public MulticastHandler(Node node, IPAddress multicastAddress) throws IOException {
        InetAddress address  = InetAddress.getByName(node.id());
        networkInterface = NetworkInterface.getByInetAddress(address);
        this.multicastAddress = new InetSocketAddress(multicastAddress.getIp(), multicastAddress.getPort());

        socket = new MulticastSocket(multicastAddress.getPort());
        socket.setNetworkInterface(networkInterface);

        socket.joinGroup(this.multicastAddress, networkInterface);
    }

    public void sendMessage(Message message) {
        byte[] bytes = message.encode();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, multicastAddress);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        running = false;
        socket.leaveGroup(multicastAddress, networkInterface);
        socket.close();
    }

    @Override
    public void run() {
        while(running) {
            byte[] buffer = new byte[MAX_BUF_LEN];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, MAX_BUF_LEN);
            try {
                socket.receive(datagramPacket);
                System.out.println(new String(datagramPacket.getData()));
            } catch (IOException e) {
                if(running) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
