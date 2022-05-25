package communication;

import message.*;
import server.MembershipService;
import server.MessageProcessor;
import server.Node;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MulticastHandler implements Runnable {
    private final MulticastSocket socket;
    private final MembershipService membershipService;
    private final InetSocketAddress multicastAddress;
    private final NetworkInterface networkInterface;
    private boolean running = true;
    private static final int MAX_BUF_LEN = 2000;

    public MulticastHandler(Node node, IPAddress multicastAddress, MembershipService service) throws IOException {
        InetAddress address  = InetAddress.getByName(node.id());
        networkInterface = NetworkInterface.getByInetAddress(address);
        if (networkInterface == null) {
            System.err.println("The specified ip address is not bound to any network interface on your machine");
            System.err.println("If you want to add it to the loopback interface, run the utility script add_lo_addr.sh");
            System.exit(1);
        }

        this.multicastAddress = new InetSocketAddress(multicastAddress.getIp(), multicastAddress.getPort());
        this.membershipService = service;

        socket = new MulticastSocket(multicastAddress.getPort());
        socket.setNetworkInterface(networkInterface);

        socket.joinGroup(this.multicastAddress, networkInterface);
    }

    public void sendMessage(Message message) throws IOException {
        byte[] bytes = message.encode();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, multicastAddress);
        socket.send(packet);
    }

    public void close() throws IOException {
        running = false;
        socket.leaveGroup(multicastAddress, networkInterface);
        socket.close();
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        while(running) {
            byte[] buffer = new byte[MAX_BUF_LEN];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, MAX_BUF_LEN);
            try {
                socket.receive(datagramPacket);

                String stringBuffer = new String(buffer, 0, datagramPacket.getLength());
                int index = stringBuffer.indexOf(MessageConstants.END_OF_LINE + MessageConstants.END_OF_LINE);
                String headers = stringBuffer.substring(0, index);
                byte[] body = stringBuffer.substring(index + MessageConstants.END_OF_LINE.length() * 2).getBytes();

                Message message = MessageFactory.createMessage(headers, body);

                MessageProcessor processor = new MessageProcessor(membershipService, message, null);
                executorService.execute(processor);
            } catch (IOException e) {
                if (running) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
