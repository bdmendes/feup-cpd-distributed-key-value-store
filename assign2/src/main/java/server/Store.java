package server;

import communication.IPAddress;
import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Store {
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
            System.exit(1);
            return;
        }

        String ipMulticast = args[0];
        String ipMulticastPort = args[1];
        String nodeId = args[2];
        int storePort;

        try {
            storePort = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("Store port must be an integer");
            System.exit(1);
            return;
        }

        Node node = new Node(nodeId, storePort);
        StorageService storageService = new StorageService(node);
        MembershipService membershipService = new MembershipService(storageService,
                new IPAddress(ipMulticast, Integer.parseInt(ipMulticastPort)));

        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(storePort)) {
            System.out.println("Store server is running on port " + storePort);

            Socket clientSocket = serverSocket.accept();
            System.out.println("Got connection from client");
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            MessageReader messageReader = new MessageReader();

            while(!messageReader.isComplete()) {
                messageReader.read(in);
            }

            Message message = MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
            MessageProcessor processor = new MessageProcessor(membershipService, message, clientSocket);
            executorService.execute(processor);
        }
        executorService.shutdown();
    }
}
