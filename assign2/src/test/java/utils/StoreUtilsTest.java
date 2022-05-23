package utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreUtilsTest {
    @Test
    void sha256Test() {
        String test = "test";
        byte[] testBytes = test.getBytes();

        String testHash = StoreUtils.sha256(testBytes);
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", testHash);
    }

    @Test
    void membershipLog() {
        MembershipLog membershipLog = new MembershipLog(null);

        membershipLog.put("node1", 0);

        assertEquals(1, membershipLog.getMap().size());
        assertEquals(0, membershipLog.get("node1"));

        membershipLog.put("node2", 0);
        membershipLog.put("node3", 0);
        membershipLog.put("node1", 1);
        membershipLog.put("node4", 0);

        Map<String,Integer> mostRecentLog = membershipLog.getMostRecentLogs(3);

        assertEquals(3, mostRecentLog.size());
        assertEquals(1, mostRecentLog.get("node1"));
        assertEquals(0, mostRecentLog.get("node3"));
        assertEquals(0, mostRecentLog.get("node4"));
        Assertions.assertNull(mostRecentLog.get("node2"));

        assertEquals(4, membershipLog.getMap().size());
        assertEquals(0, membershipLog.get("node2"));

        byte[] data = MembershipLog.writeMembershipLogToData(membershipLog.getMap());

        membershipLog = new MembershipLog(null);

        MembershipLog.readMembershipLogFromData(membershipLog.getMap(), data);

        mostRecentLog = membershipLog.getMostRecentLogs(3);

        assertEquals(3, mostRecentLog.size());
        assertEquals(1, mostRecentLog.get("node1"));
        assertEquals(0, mostRecentLog.get("node3"));
        assertEquals(0, mostRecentLog.get("node4"));
        Assertions.assertNull(mostRecentLog.get("node2"));

        assertEquals(4, membershipLog.getMap().size());
        assertEquals(0, membershipLog.get("node2"));
    }

    @Test
    void sha25d6Test() {
        LinkedHashMap<String, String> test = new LinkedHashMap<>();

        test.put("test", "test");

        for (Map.Entry<String, String> entry : test.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        test.put("test2", "test2");
        test.put("test3", "test2");

        for (Map.Entry<String, String> entry : test.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        test.put("test", "test");

        for (Map.Entry<String, String> entry : test.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

    }
}