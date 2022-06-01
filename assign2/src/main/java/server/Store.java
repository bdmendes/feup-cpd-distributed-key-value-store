package server;

import communication.IPAddress;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Store {
    public static boolean removeLocalStorage(StorageService storageService) {
        File directory = new File(storageService.getStorageDirectory());
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }

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
            try {
                registry.bind(registryName, stub);
            } catch (java.rmi.AlreadyBoundException alreadyBoundException) {
                System.err.println("IDs must be unique in this cluster. This node is invalid. Exiting...");
                removeLocalStorage(membershipService.getStorageService());
                System.exit(1);
            }
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
        int storePort = -1;

        try {
            storePort = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("Store port must be an integer");
            System.exit(1);
        }

        Node node = new Node(nodeId, storePort);
        if (node.getNetworkInterfaceBindToIP() == null) {
            System.err.println("The specified ip address is not bound to any network interface on your machine");
            System.err.println("If you want to add it to the loopback interface, run the utility script add_lo_addr.sh <nodeIP>");
            System.exit(1);
        }

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
