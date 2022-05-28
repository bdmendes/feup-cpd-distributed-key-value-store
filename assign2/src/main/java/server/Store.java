package server;

import communication.IPAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

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
        try {
            ServerSocket receiveSocket = new ServerSocket();
            receiveSocket.bind(new InetSocketAddress(nodeId, storePort));

            MembershipService membershipService = new MembershipService(
                    storageService,
                    new IPAddress(ipMulticast, Integer.parseInt(ipMulticastPort)),
                    receiveSocket
            );

            System.out.println("Store server is running on " + nodeId + ":" + storePort);
            System.out.println("Current node membership counter: " + membershipService.getMembershipCounter().get());

            Store.bindRmiMethods(membershipService);
        } catch (IOException e) {
            System.out.println("Failed to create server socket");
            e.printStackTrace();
        }
    }
}
