package utils;

import message.MessageConstants;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MembershipLog {
    private final Map<String, Integer> membershipLog;
    private final String filePath;

    public MembershipLog(String filePath) {
        membershipLog = Collections.synchronizedMap(new LinkedHashMap<>(
                32, .75f, false));
        this.filePath = filePath;
        this.readFromFile();
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
        membershipLog.forEach((key, value) -> stringBuilder.append(key)
                .append(" ")
                .append(value)
                .append(MessageConstants.END_OF_LINE));

        return stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Integer> getMap() {
        return membershipLog;
    }

    public synchronized Integer put(String nodeId, Integer nodeMembershipCounter) {
        membershipLog.remove(nodeId);
        Integer status = membershipLog.put(nodeId, nodeMembershipCounter);
        writeToFile();
        return status;
    }

    public Integer get(String nodeId) {
        return membershipLog.get(nodeId);
    }

    public synchronized void clear() {
        membershipLog.clear();
        this.writeToFile();
    }

    public int totalCounter() {
        Collection<Integer> values = membershipLog.values();

        synchronized (membershipLog) {
            return values.stream().mapToInt(Integer::intValue).sum();
        }
    }

    public Map<String, Integer> getMostRecentLogs(int numberOfLogs) {
        Map<String, Integer> mostRecentLogs = new LinkedHashMap<>();
        int counter = 0;

        synchronized (membershipLog) {
            int size = membershipLog.size();
            int start = size - numberOfLogs;

            for (Map.Entry<String, Integer> entry : membershipLog.entrySet()) {
                if (counter >= start) {
                    mostRecentLogs.put(entry.getKey(), entry.getValue());
                }
                counter++;
            }
        }

        return mostRecentLogs;
    }

    private void readFromFile() {
        if (filePath == null) {
            return;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(filePath));
            MembershipLog.readMembershipLogFromData(membershipLog, bytes);
        } catch (IOException e) {
            this.writeToFile();
        }
    }

    private void writeToFile() {
        if (filePath == null) {
            return;
        }
        byte[] bytes = MembershipLog.writeMembershipLogToData(membershipLog);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
