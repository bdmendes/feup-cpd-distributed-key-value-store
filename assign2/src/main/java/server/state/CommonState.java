package server.state;

import communication.CommunicationUtils;
import message.*;
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
            return;
        }

        Map<String, Integer> recentLogs = membershipService.getMembershipLog(32);
        for (Node node : membershipMessage.getNodes()) {
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

    public static void processLocalPut(PutMessage putMessage, Socket clientSocket, MembershipService membershipService) {
        PutReply response = new PutReply();
        response.setKey(putMessage.getKey());
        try {
            membershipService.getStorageService().put(putMessage.getKey(), putMessage.getValue());
            response.setStatusCode(StatusCode.OK);
        } catch (IOException e) {
            response.setStatusCode(StatusCode.ERROR);
        }
        System.out.println("Putting hash " + putMessage.getKey());
        CommunicationUtils.sendMessage(response, clientSocket);
    }
}
