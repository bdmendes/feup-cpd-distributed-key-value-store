package server;

import java.io.IOException;

public class Store {
    public static void main(String[] args) throws IOException {
        if(args.length != 4) {
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
        byte[] test = {64, 65};
        storageService.put("key1", test);
        storageService.put("key2", new byte[]{67, 67, 69});
        storageService.put("key3", test);

        var str = "ola";
        System.out.println(MembershipService.sha256(str));
    }
}
