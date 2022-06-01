package client;

import communication.IPAddress;
import communication.RMIAddress;
import message.*;
import message.messagereader.MessageReader;
import server.MembershipRMI;
import utils.StoreUtils;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {
    private static void printUsage() {
        System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
    }

    private static ClientArgs parseArgs(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Not enough arguments");
        }

        String nodeAccessPoint = args[0];
        String operation = args[1];
        String operand;
        ClientArgs clientArgs;

        if (!operation.equals("join") && !operation.equals("leave")) {
            if (args.length != 3) {
                throw new IllegalArgumentException("Missing operand");
            }
            operand = args[2];
            clientArgs = new ClientArgs(nodeAccessPoint, operation, operand);
        } else {
            if (args.length != 2) {
                throw new IllegalArgumentException("Invalid number of arguments");
            }
            clientArgs = new ClientArgs(nodeAccessPoint, operation);
        }

        return clientArgs;
    }

    private static Message createMessage(ClientArgs clientArgs) {
        Message msg;
        String operation = clientArgs.operation;
        String operand = clientArgs.operand;

        switch (operation) {
            case "put" -> {
                if (operand == null) {
                    throw new IllegalArgumentException("Missing operand");
                }
                PutMessage putMessage = new PutMessage();
                try {
                    File file = new File(operand);
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String key = StoreUtils.sha256(bytes);

                    putMessage.setKey(key);
                    putMessage.setValue(bytes);

                    msg = putMessage;
                } catch (IOException e) {
                    throw new IllegalArgumentException("File not found");
                }
            }
            case "get" -> {
                if (operand == null) {
                    throw new IllegalArgumentException("Missing operand");
                }

                GetMessage getMessage = new GetMessage();
                getMessage.setKey(operand);

                msg = getMessage;
            }
            case "delete" -> {
                if (operand == null) {
                    throw new IllegalArgumentException("Missing operand");
                }

                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setKey(operand);

                msg = deleteMessage;
            }
            default -> throw new IllegalArgumentException("Invalid operation");
        }

        return msg;
    }

    public static void main(String[] args) throws java.io.IOException {
        ClientArgs clientArgs;
        try {
            clientArgs = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            printUsage();
            System.exit(1);
            return;
        }

        if (clientArgs.operation.equals("join") || clientArgs.operation.equals("leave")) {
            RMIAddress nodeAccessPoint;
            try {
                nodeAccessPoint = new RMIAddress(clientArgs.host);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printUsage();
                System.exit(1);
                return;
            }

            try {
                Registry registry = LocateRegistry.getRegistry(nodeAccessPoint.getIp());
                MembershipRMI stub = (MembershipRMI) registry.lookup(nodeAccessPoint.getObjectName());
                MembershipRMI.Status response = clientArgs.operation.equals("join") ? stub.join() : stub.leave();
                System.out.println("response: " + response);
            } catch (Exception e) {
                System.err.println("RMI error: " + e);
                System.exit(1);
            }

        } else {
            Message msg;
            try {
                msg = createMessage(clientArgs);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printUsage();
                System.exit(1);
                return;
            }
            IPAddress nodeAccessPoint;
            try {
                nodeAccessPoint = new IPAddress(clientArgs.host);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printUsage();
                System.exit(1);
                return;
            }

            try (Socket socket = new Socket(nodeAccessPoint.getIp(), nodeAccessPoint.getPort())) {
                socket.setSoTimeout(1000);
                OutputStream output = socket.getOutputStream();
                output.write(msg.encode());

                ClientMessageVisitor visitor = new ClientMessageVisitor();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                MessageReader messageReader = new MessageReader();

                while (!messageReader.isComplete()) {
                    messageReader.read(in);
                }

                Message message = MessageFactory.createMessage(messageReader.getHeader(), messageReader.getBody());
                visitor.process(message, socket);
            } catch (Exception e) {
                System.out.println("Could not connect to client: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private record ClientArgs(String host, String operation, String operand) {
        public ClientArgs(String host, String operation) {
            this(host, operation, null);
        }
    }
}
