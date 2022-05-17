package server;

import message.*;
import utils.MembershipLog;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MembershipService implements MessageVisitor {
    private final StorageService storageService;
    private int nodeMembershipCounter;
    private Map<String, Integer> membershipLog;
    private final InetAddress ipMulticastGroup;

    public MembershipService(StorageService storageService, InetAddress ipMulticastGroup) {
        this.storageService = storageService;
        this.ipMulticastGroup = ipMulticastGroup;
        this.membershipLog = MembershipLog.generateMembershipLog();
        this.readMembershipCounterFromFile();
        this.readMembershipLogFromFile();
    }

    public InetAddress getIpMulticastGroup() {
        return ipMulticastGroup;
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

        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(Path.of(getMembershipLogFilePath()));
            MembershipLog.readMembershipLogFromData(membershipLog, bytes);
        } catch (IOException e) {
            this.writeMembershipLogToFile();
        }
    }

    protected void writeMembershipLogToFile() {
        byte[] bytes = MembershipLog.writeMembershipLogToData(this.membershipLog);
        try (FileOutputStream fos = new FileOutputStream(getMembershipLogFilePath())) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void multicastMembershipLog() {
        //
    }

    private void multicastJoinLeave() {
        JoinMessage message = new JoinMessage();
        message.setCounter(nodeMembershipCounter);
        message.setNodeId(storageService.getNode().id());
        incrementCounter();
    }


    public void joinCluster() {
        if (nodeMembershipCounter % 2 != 0) {
            return;
        }

        this.multicastJoinLeave();
    }

    public void leaveCluster() {
        if (nodeMembershipCounter % 2 == 0) {
            return;
        }

        this.multicastJoinLeave();
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
