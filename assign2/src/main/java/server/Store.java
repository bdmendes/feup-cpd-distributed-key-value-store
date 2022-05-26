package server;

import communication.IPAddress;
import message.Message;
import message.MessageFactory;
import message.messagereader.MessageReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Store {
    public static void bindRmiMethods(MembershipService membershipService) {
        try {
            MembershipRMI stub = (MembershipRMI) UnicastRemoteObject.exportObject(membershipService, 0);
            Registry registryTemp;
            try {
                registryTemp = LocateRegistry.createRegistry(1099);
            } catch (RemoteException e) {
                registryTemp = LocateRegistry.getRegistry();
            }
            final Registry registry = registryTemp;

            String registryName = "reg" + membershipService.getStorageService().getNode().id();
            registry.rebind(registryName, stub);
            System.err.println("Server ready for RMI operations on registry: " + registryName);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    registry.unbind("reg" + membershipService.getStorageService().getNode().id());
                } catch (RemoteException | NotBoundException e) {
                    System.err.println("Could not shutdown executor service");
                    e.printStackTrace();
                }
            }));
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

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(nodeId, storePort));
            System.out.println("Store server is running on " + nodeId + ":" + storePort);
            System.out.println("Current node membership counter: " + membershipService.getMembershipCounter().get());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    executorService.shutdown();
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Got connection from client");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                MessageReader messageReader = new MessageReader();

                while (!messageReader.isComplete()) {
                    messageReader.read(in);
                }

                Message message = MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
                MessageProcessor processor = new MessageProcessor(membershipService, message, clientSocket);
                executorService.execute(processor);
            }
        }
    }
}
