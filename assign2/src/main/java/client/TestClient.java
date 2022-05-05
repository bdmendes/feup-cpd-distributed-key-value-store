package client;

import message.Message;
import message.MessageConstants;
import message.PutMessage;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TestClient {
    private static void printUsage() {
        System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
    }

    public static void main(String[] args) throws java.io.IOException {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String nodeAccessPoint = args[0];
        String operation = args[1];
        String operand;
        if (!operation.equals("join") && !operation.equals("leave")) {
            if (args.length != 3) {
                printUsage();
                System.exit(1);
            }
            operand = args[2];
        } else {
            if (args.length != 2) {
                printUsage();
                System.exit(1);
            }
        }

        // assume put for now
        // send tcp message
        String value = "bdmendes\nCool Stuff!!" + MessageConstants.END_OF_LINE + "Beans everywhere!";
        byte[] arr = value.getBytes(StandardCharsets.UTF_8);
        PutMessage msg = new PutMessage();
        msg.setKey("key34");
        msg.setValue(arr);
        try(Socket socket = new Socket("127.0.0.1", 9000)) {
            OutputStream output = socket.getOutputStream();
            output.write(msg.encode());
        };
    }
}
