package server;

import communication.IPAddress;
import message.MembershipMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.state.CommonState;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MembershipServiceTest {

    @BeforeEach
    void deleteTestAssetsBeforeEach() throws IOException {
        deleteTestAssets();
    }

    //@AfterAll
    static void deleteTestAssets() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        File directory = new File(storageService.getStorageDirectory());
        if (directory.exists()){
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Test
    void testGetPut() throws IOException {
        MembershipService service;
        MembershipService service2;
        MembershipService service3;

        StorageService storageService = new StorageService(new Node("-1", -1));
        service = new MembershipService(storageService);

        service.getMembershipCounter().incrementAndGet();
        service.getMembershipCounter().incrementAndGet();

        assertEquals(1, service.getNodeMembershipCounter());

        service2 = new MembershipService(storageService);

        assertEquals(1, service2.getNodeMembershipCounter());

        service2.getMembershipCounter().incrementAndGet();

        service3 = new MembershipService(storageService);
        assertEquals(2, service3.getNodeMembershipCounter());
    }


    @Test
    void testLogPutGet() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService service = new MembershipService(storageService);

        service.getMembershipLog().put("0", 0);
        service.getMembershipLog().put("2", 3);

        Map<String, Integer> membershipLog = service.getMembershipLog(32);
        assertEquals(2, membershipLog.size());
        assertTrue(membershipLog.containsKey("0"));
        assertTrue(membershipLog.containsKey("2"));

        assertEquals(
                membershipLog.values().stream().toList(),
                List.of(new Integer[]{0, 3})
        );

        MembershipService service2 = new MembershipService(storageService);

        membershipLog = service2.getMembershipLog(32);
        assertEquals(2, membershipLog.size());
        assertTrue(membershipLog.containsKey("0"));
        assertTrue(membershipLog.containsKey("2"));

        assertEquals(
                membershipLog.values().stream().toList(),
                List.of(new Integer[]{0, 3})
        );
    }

    @Test
    void testElderyRemoval() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService service = new MembershipService(storageService);

        for (int i = 0; i < 40; i++) {
            service.getMembershipLog().put(Integer.toString(i), i);
        }

        var valuesList = service.getMembershipLog(32).values().stream().toList();
        for (int i = 0; i < 32; i++) {
            assertEquals(valuesList.get(i), i + 8);
        }
    }

    @Test
    void testMembershipMergeNewNode() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService membershipService = new MembershipService(storageService);
        membershipService.getClusterMap().put(new Node("1", -1));
        membershipService.getMembershipLog().put("1", 0);

        Set<Node> messageNodes = new HashSet<>();
        messageNodes.add(new Node("1", -1));
        messageNodes.add(new Node("2", -1));
        Map<String, Integer> messageEvents = new HashMap<>();
        messageEvents.put("1", 0);
        messageEvents.put("2", 0);
        MembershipMessage membershipMessage = new MembershipMessage();
        membershipMessage.setNodes(messageNodes);
        membershipMessage.setMembershipLog(messageEvents);

        CommonState.processMembership(membershipMessage, membershipService);
        assertEquals(membershipService.getClusterMap().getNodes().size(), 2);
        assertEquals(membershipService.getMembershipLog(32).size(), 2);
        assertEquals(membershipService.getMembershipLog(32).get("2"), 0);
    }

    @Test
    void testMembershipMergeKnownNodeLeft() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService membershipService = new MembershipService(storageService);
        membershipService.getClusterMap().put(new Node("1", -1));
        membershipService.getClusterMap().put(new Node("2", -1));
        membershipService.getMembershipLog().put("1", 0);
        membershipService.getMembershipLog().put("2", 0);

        Set<Node> messageNodes = new HashSet<>();
        messageNodes.add(new Node("1", -1));
        Map<String, Integer> messageEvents = new HashMap<>();
        messageEvents.put("1", 0);
        messageEvents.put("2", 1);
        MembershipMessage membershipMessage = new MembershipMessage();
        membershipMessage.setNodes(messageNodes);
        membershipMessage.setMembershipLog(messageEvents);

        CommonState.processMembership(membershipMessage, membershipService);
        assertEquals(membershipService.getClusterMap().getNodes().size(), 1);
        assertEquals(membershipService.getMembershipLog(32).get("2"), 1);
    }

    @Test
    void testMembershipMergeKnownNodeJoined() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService membershipService = new MembershipService(storageService);
        membershipService.getClusterMap().put(new Node("1", -1));
        membershipService.getMembershipLog().put("1", 0);
        membershipService.getMembershipLog().put("2", 1);

        Set<Node> messageNodes = new HashSet<>();
        messageNodes.add(new Node("1", -1));
        messageNodes.add(new Node("2", -1));
        Map<String, Integer> messageEvents = new HashMap<>();
        messageEvents.put("1", 0);
        messageEvents.put("2", 2);
        MembershipMessage membershipMessage = new MembershipMessage();
        membershipMessage.setNodes(messageNodes);
        membershipMessage.setMembershipLog(messageEvents);

        CommonState.processMembership(membershipMessage, membershipService);
        assertEquals(membershipService.getClusterMap().getNodes().size(), 2);
        assertEquals(membershipService.getMembershipLog(32).get("2"), 2);
    }

    @Test
    void testMembershipMergeOlderNodeEvent() throws IOException {
        StorageService storageService = new StorageService(new Node("-1", -1));
        MembershipService membershipService = new MembershipService(storageService);
        membershipService.getClusterMap().put(new Node("1", -1));
        membershipService.getClusterMap().put(new Node("2", -1));
        membershipService.getMembershipLog().put("1", 0);
        membershipService.getMembershipLog().put("2", 2);

        Set<Node> messageNodes = new HashSet<>();
        messageNodes.add(new Node("1", -1));
        Map<String, Integer> messageEvents = new HashMap<>();
        messageEvents.put("1", 0);
        messageEvents.put("2", 1);
        MembershipMessage membershipMessage = new MembershipMessage();
        membershipMessage.setNodes(messageNodes);
        membershipMessage.setMembershipLog(messageEvents);

        CommonState.processMembership(membershipMessage, membershipService);
        assertEquals(membershipService.getClusterMap().getNodes().size(), 2);
        assertEquals(membershipService.getMembershipLog(32).get("2"), 2);
    }
}
