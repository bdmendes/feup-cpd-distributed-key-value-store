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

            if (nodeId.equals(membershipService.getStorageService().getNode().id())
                    && membershipCounter > membershipService.getNodeMembershipCounter()) {
                while (membershipCounter > membershipService.getNodeMembershipCounter()) {
                    membershipService.getMembershipCounter().incrementAndGet();
                }
                membershipService.setNodeState(new InitNodeState(membershipService));
                membershipService.join();
                continue;
            }

            boolean containsEventFromNode = recentLogs.containsKey(nodeId);
            boolean newerThanLocalEvent = containsEventFromNode
                    && recentLogs.get(nodeId) < membershipCounter;
            if (!containsEventFromNode || newerThanLocalEvent) {
                membershipService.getMembershipLog().put(nodeId, membershipCounter);
                boolean nodeJoined = membershipCounter % 2 == 0;
                Optional<Node> node = membershipMessage.getNodes().stream().filter(n -> n.id().equals(nodeId)).findFirst();
                if (node.isEmpty()) {
                    continue;
                }
                if (nodeJoined) {
                    membershipService.getClusterMap().put(node.get());
                    if (membershipService.getNodeState().joined()) {
                        membershipService.transferKeysToJoiningNode(node.get());
                    }
                } else {
                    membershipService.removeUnavailableNode(node.get());
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

                if (putMessage.isTransference() && membershipService.getStorageService().getTombstones().contains(key)) {
                    continue;
                }

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

    public static void processDeleteRelay(DeleteRelayMessage deleteRelayMessage, Socket clientSocket, MembershipService membershipService) {
        boolean deleted;
        synchronized (membershipService.getStorageService().getHashLock(deleteRelayMessage.getKey())) {
            deleted = membershipService.getStorageService().delete(deleteRelayMessage.getKey(), !deleteRelayMessage.isTransference());
        }
        System.out.println("Deleting hash " + deleteRelayMessage.getKey());
        DeleteRelayReply response = new DeleteRelayReply();
        response.reportSuccess(deleteRelayMessage.getKey());
        response.setStatusCode(deleted ? StatusCode.OK : StatusCode.FILE_NOT_FOUND);
        CommunicationUtils.sendMessage(response, clientSocket);
    }
}
