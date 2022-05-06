package client;

import message.*;
import utils.StoreUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;

public class TestClient {
    private record ClientArgs(String host, String operation, String operand) {
        public ClientArgs(String host, String operation) {
            this(host, operation, null);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
    }

    private static ClientArgs parseArgs(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Not enough arguments");
        }

        String nodeAccessPoint = args[0];
        String operation = args[1];
        String operand = null;
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
            default -> {
                throw new IllegalArgumentException("Invalid operation");
            }
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

        Message msg;
        try {
            msg = createMessage(clientArgs);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            printUsage();
            System.exit(1);
            return;
        }

        if(clientArgs.operation.equals("join") || clientArgs.operation.equals("leave")) {
            RMIAddress nodeAccessPoint;
            try {
                nodeAccessPoint = new RMIAddress(clientArgs.host);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printUsage();
                System.exit(1);
                return;
            }

            // membership stuff
        } else {
            TCPAddress nodeAccessPoint;
            try {
                nodeAccessPoint = new TCPAddress(clientArgs.host);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                printUsage();
                System.exit(1);
                return;
            }

            try(Socket socket = new Socket(nodeAccessPoint.getIp(), nodeAccessPoint.getPort())) {
                OutputStream output = socket.getOutputStream();
                output.write(msg.encode());
            } catch (UnknownHostException e) {
                System.out.println("Unknown host");
                System.exit(1);
            }
        }
    }
}
