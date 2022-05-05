package server;

import message.Message;
import message.PutMessage;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static message.MessageConstants.END_OF_LINE;

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
        Node node = new Node(nodeId, 9000);
        StorageService storageService = new StorageService(node);
        MembershipService membershipService = new MembershipService(storageService);

        try (ServerSocket serverSocket = new ServerSocket(9000)) {
            System.out.println("Store server is running on port " + 9000);

            System.out.println("Got connection from client");
            Socket clientSocket = serverSocket.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            DataInputStream inData = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            StringBuilder msg = new StringBuilder();
            for(;;) {
                String line = in.readLine();
                if (line == null) break;
                else msg.append(line + END_OF_LINE);
            }
            System.out.println("Received message: " + msg);
            //Message message = new PutMessage(msg.toString());
            byte[] data = new byte[100];
            int s = inData.read(data);
            System.out.println("Received data: " + s);
            System.out.println("Received data: " + new String(data));
            //membershipService.process(message);
        }
    }
}
