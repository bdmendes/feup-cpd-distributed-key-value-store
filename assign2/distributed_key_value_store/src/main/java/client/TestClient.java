package client;

import message.Message;
import message.PutMessage;

public class TestClient {
    private static void printUsage() {
        System.out.println("Usage: java TestClient <node_ap> <operation> [<opnd>]");
    }

    public static void main(String[] args) {
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
        Message msg = PutMessage()
    }
}
