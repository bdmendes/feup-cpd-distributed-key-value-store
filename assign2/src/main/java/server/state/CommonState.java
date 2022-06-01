package server.state;

import communication.CommunicationUtils;
import message.MembershipMessage;
import message.PutRelayMessage;
import message.PutRelayReply;
import message.StatusCode;
import server.MembershipService;
import server.Node;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;

public class CommonState {
    public static void processMembership(MembershipMessage membershipMessage, MembershipService membershipService) {
        if (membershipMessage.getNodeId().equals(membershipService.getStorageService().getNode().id())) {
            System.out.println("Received membership message from myself");
        }

        Map<String, Integer> recentLogs = membershipService.getMembershipLog(32);
        for (Node node : membershipMessage.getNodes()) {
            if (node.id().equals(membershipService.getStorageService().getNode().id())
                    && node.port() != membershipService.getStorageService().getNode().port()) {
                System.err.println("IDs must be unique in this cluster. This node is invalid. Exiting...");
                System.exit(1);
            }
            boolean loggedRecently = recentLogs.containsKey(node.id());
            if (!loggedRecently) {
                membershipService.getClusterMap().put(node);
            }
        }

        for (Map.Entry<String, Integer> entry : membershipMessage.getMembershipLog().entrySet()) {
            String nodeId = entry.getKey();
            Integer membershipCounter = entry.getValue();
            boolean containsEventFromNode = recentLogs.containsKey(nodeId);
            boolean newerThanLocalEvent = containsEventFromNode
                    && recentLogs.get(nodeId) < membershipCounter;
            if (!containsEventFromNode || newerThanLocalEvent) {
                membershipService.getMembershipLog().put(nodeId, membershipCounter);
                boolean nodeJoined = membershipCounter % 2 == 0;
                if (nodeJoined) {
                    Optional<Node> node = membershipMessage.getNodes().stream().filter(n -> n.id().equals(nodeId)).findFirst();
                    if (node.isEmpty()) continue;
                    membershipService.getClusterMap().put(node.get());
                } else {
                    membershipService.getClusterMap().removeId(nodeId);
                }
            }
        }

        System.out.println("Received membership message " + membershipService.getMembershipLog().getMap().entrySet());
    }

    public static void processPutRelay(PutRelayMessage putMessage, Socket clientSocket, MembershipService membershipService) {
        System.out.println("Received put relay message ");
        PutRelayReply response = new PutRelayReply();

        try {
            for (Map.Entry<String, byte[]> entry : putMessage.getValues().entrySet()) {
                String key = entry.getKey();
                byte[] value = entry.getValue();
                System.out.println("Putting hash " + key);

                membershipService.getStorageService().put(key, value);
                response.reportSuccess(key);
            }

            response.setStatusCode(StatusCode.OK);
        } catch (IOException e) {
            System.out.println("Error storing file");

            response.setStatusCode(StatusCode.ERROR);
        }
        CommunicationUtils.sendMessage(response, clientSocket);
    }
}
