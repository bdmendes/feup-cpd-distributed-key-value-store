package message;

import utils.MembershipLog;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ElectionMessage extends Message {

    private Map<String, Integer> membershipLog;
    private String origin;

    public ElectionMessage() {
    }

    public ElectionMessage(String headers, byte[] data) {
        membershipLog = new HashMap<>();
        Map<String, String> fields = decodeFields(headers);
        origin = fields.get("origin");

        MembershipLog.readMembershipLogFromData(membershipLog, data);
    }

    @Override
    public byte[] encode() {
        byte[] data = MembershipLog.writeMembershipLogToData(membershipLog);
        Map<String, String> fields = new HashMap<>();
        fields.put("origin", origin);

        return encodeWithFields(MessageType.ELECTION, fields, data);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processElection(this);
    }

    public Map<String, Integer> getMembershipLog() {
        return membershipLog;
    }

    public void setMembershipLog(Map<String, Integer> membershipLog) {
        this.membershipLog = membershipLog;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}