package server;

import message.*;
import utils.StoreUtils;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class MembershipService implements MessageVisitor {
    private final StorageService storageService;
    private int nodeMembershipCounter;
    private final Map<String, Integer> membershipLog;

    public MembershipService(StorageService storageService) {
        this.storageService = storageService;
        this.membershipLog = Collections.synchronizedMap(new LinkedHashMap<>(
                32, .75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
                return this.size() > 32;
            }
        });
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
    }

    public Map<String, Integer> getMembershipLog() {
        return membershipLog;
    }

    protected void readMembershipCounterFromFile() {
        try {
            Scanner scanner = new Scanner(new File(getMembershipCounterFilePath()));
            nodeMembershipCounter = scanner.nextInt();
            scanner.close();
        } catch (Exception e) {
            nodeMembershipCounter = 0;
            this.writeMembershipCounterToFile();
        }
    }

    protected void writeMembershipCounterToFile() {
        try {
            FileWriter fileWriter = new FileWriter(getMembershipCounterFilePath());
            fileWriter.write(Integer.toString(nodeMembershipCounter));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void readMembershipLogFromFile() {
        try {
            Scanner scanner = new Scanner(new File(getMembershipLogFilePath()));
            while (scanner.hasNextLine()){
                String[] line = scanner.nextLine().split(" ");
                String nodeId = line[0];
                int counter = Integer.parseInt(line[1]);
                membershipLog.put(nodeId, counter);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            this.writeMembershipLogToFile();
        }
    }

    protected void writeMembershipLogToFile() {
        try {
            FileWriter fileWriter = new FileWriter(getMembershipLogFilePath());
            this.membershipLog.forEach((key, value) -> {
                try {
                    fileWriter.write(key + " " + value);
                    fileWriter.write(MessageConstants.END_OF_LINE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void multicastMembershipLog() {
        //
    }

    private void joinCluster() {

    }

    private void leaveCluster() {

    }

    public int getNodeMembershipCounter() {
        return nodeMembershipCounter;
    }

    @Override
    public void processPut(PutMessage putMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN STORE KEY/VALUE PAIR:
        PutReply response = new PutReply();
        response.setKey(putMessage.getKey());

        try {
            storageService.put(putMessage.getKey(), putMessage.getValue());
            response.setStatusCode(StatusCode.OK);
        } catch (IOException e) {
            response.setStatusCode(StatusCode.ERROR);
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            throw new RuntimeException("Could not send put reply");
        }
    }

    @Override
    public void processGet(GetMessage getMessage, Socket socket) {
        // FIND NODE TO STORE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN GET KEY/VALUE PAIR:
        GetReply response = new GetReply();
        response.setKey(getMessage.getKey());

        try {
            byte[] value = storageService.get(getMessage.getKey());

            response.setValue(value);
            response.setStatusCode(StatusCode.OK);

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            response.setStatusCode(StatusCode.FILE_NOT_FOUND);
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            throw new RuntimeException("Could not send get reply");
        }
    }

    @Override
    public void processDelete(DeleteMessage deleteMessage, Socket socket) {
        // FIND NODE TO DELETE KEY/VALUE PAIR

        // IF IS THE CORRECT NODE - THEN DELETE KEY/VALUE PAIR:
        boolean deleted = storageService.delete(deleteMessage.getKey());
        DeleteReply response = new DeleteReply();
        response.setKey(deleteMessage.getKey());

        if (!deleted) {
            response.setStatusCode(StatusCode.FILE_NOT_FOUND);
        } else {
            response.setStatusCode(StatusCode.OK);
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(response.encode());
        } catch (IOException e) {
            throw new RuntimeException("Could not send delete reply");
        }
    }

    @Override
    public void processMembership(MembershipMessage membershipMessage, Socket socket) {

    }

    @Override
    public void processJoin(JoinMessage joinMessage, Socket socket) {

    }

    @Override
    public void processLeave(LeaveMessage leaveMessage, Socket socket) {

    }

    @Override
    public void processGetReply(GetReply getReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(getReply.encode());
    }

    @Override
    public void processPutReply(PutReply putReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(putReply.encode());
    }

    @Override
    public void processDeleteReply(DeleteReply deleteReply, Socket socket) throws IOException {
        // propagate

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(deleteReply.encode());
    }

    @Override
    public void process(Message message, Socket socket) throws IOException {
        message.accept(this, socket);
    }

    private String getMembershipCounterFilePath() {
        return "./node_storage/storage" + storageService.getNode() + "/membership_counter.txt";
    }

    private String getMembershipLogFilePath() {
        return "./node_storage/storage" + storageService.getNode() + "/membership_log.txt";
    }

    protected void addMembershipEvent(String nodeId, int membershipCounter){
        membershipLog.put(nodeId, membershipCounter);
        this.writeMembershipLogToFile();
    }

    protected void incrementCounter() {
        this.nodeMembershipCounter++;
        this.writeMembershipCounterToFile();
    }
}
