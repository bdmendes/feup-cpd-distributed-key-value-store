package utils;

import message.MessageConstants;

import java.io.IOException;
import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MembershipLog {
    private final Map<String, Integer> membershipLog;

    public MembershipLog() {
        membershipLog = Collections.synchronizedMap(new LinkedHashMap<>(
                32, .75f, false));
    }
    public Map<String, Integer> getMap() {
        return membershipLog;
    }

    public Integer put(String nodeId, Integer nodeMembershipCounter) {
        membershipLog.remove(nodeId);
        return membershipLog.put(nodeId, nodeMembershipCounter);
    }

    public Integer get(String nodeId) {
        return membershipLog.get(nodeId);
    }

    public void clear() {
        membershipLog.clear();
    }

    public int totalCounter() {
        return membershipLog.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<String, Integer> getMostRecentLogs(int numberOfLogs) {
        Map<String, Integer> mostRecentLogs = new LinkedHashMap<>();
        int counter = 0;
        int size = membershipLog.size();
        int start = size - numberOfLogs;

        for (Map.Entry<String, Integer> entry : membershipLog.entrySet()) {
            if (counter >= start) {
                mostRecentLogs.put(entry.getKey(), entry.getValue());
            }
            counter++;
        }
        return mostRecentLogs;
    }

    public static void readMembershipLogFromData(Map<String, Integer> membershipLog, byte[] data) {
        Scanner scanner = new Scanner(new String(data, StandardCharsets.UTF_8));
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(" ");
            String nodeId = line[0];
            int counter = Integer.parseInt(line[1]);
            membershipLog.put(nodeId, counter);
        }
        scanner.close();
    }

    public static byte[] writeMembershipLogToData(Map<String, Integer> membershipLog) {
        StringBuilder stringBuilder = new StringBuilder();
        membershipLog.forEach((key, value) -> {
            stringBuilder.append(key)
                    .append(" ")
                    .append(value)
                    .append(MessageConstants.END_OF_LINE);
        });

        return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
