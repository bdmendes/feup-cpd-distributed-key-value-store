package server;

import communication.IPAddress;
import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Store {
    public static void bindRmiMethods(MembershipService membershipService){
        try {
            MembershipRMI stub = (MembershipRMI) UnicastRemoteObject.exportObject(membershipService, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("reg" + membershipService.getStorageService().getNode().id(), stub);
            System.err.println("Server ready for RMI operations");
        } catch (Exception e) {
            System.err.println("Could not bind membership service stub to RMI registry: " + e);
            e.printStackTrace();
        }
    }

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
        Store.bindRmiMethods(membershipService);

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // TCP LISTENER
        // ADD UDP THREAD TO PROCESS MULTICAST JOINS AND LEAVES
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
