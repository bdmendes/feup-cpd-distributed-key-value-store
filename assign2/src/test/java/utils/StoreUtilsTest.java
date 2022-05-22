package utils;

import org.junit.jupiter.api.Test;

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
    void testMap() {
        Map<String, Integer> map = Collections.synchronizedMap(new LinkedHashMap<String, Integer>(
                3, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return this.size() > 3;
            }
        });

        System.out.println(map.size());

        map.put("node1", 0);
        map.put("node2", 0);
        map.put("node3", 0);
        map.put("node1", 1);
        map.put("node4", 0);
    }

    @Test
    void membershipLog() {
        Map<String, Integer> membershipLog = MembershipLog.generateMembershipLog();


        membershipLog.put("node1", 0);

        System.out.println(MembershipLog.getMostRecentLogs(membershipLog, 3));

        membershipLog.put("node2", 0);
        membershipLog.put("node3", 0);
        membershipLog.put("node1", 1);
        membershipLog.put("node4", 0);

        System.out.println(MembershipLog.getMostRecentLogs(membershipLog, 3));
        System.out.println(membershipLog);
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