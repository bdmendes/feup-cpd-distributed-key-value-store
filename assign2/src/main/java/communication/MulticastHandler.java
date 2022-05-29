package communication;

import message.Message;
import message.MessageConstants;
import message.MessageFactory;
import server.MembershipService;
import server.MessageProcessor;
import server.Node;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MulticastHandler implements Runnable {
    private static final int MAX_BUF_LEN = 2000;
    private final MulticastSocket socket;
    private final MembershipService membershipService;
    private final InetSocketAddress multicastAddress;
    private final NetworkInterface networkInterface;
    private ExecutorService executorService;
    private boolean running = true;

    public MulticastHandler(Node node, IPAddress multicastAddress, MembershipService service) throws IOException {
        networkInterface = node.getNetworkInterfaceBindToIP();

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

    public void waitTasks() throws InterruptedException {
        running = false;

        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public void close() throws IOException {
        socket.leaveGroup(multicastAddress, networkInterface);
        socket.close();
    }

    @Override
    public void run() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() / 2);

        while (running) {
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
