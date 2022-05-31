package message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class LeaderMessage extends Message {

    private String leaderNode;

    public LeaderMessage() {
    }

    public LeaderMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        leaderNode = fields.get("leader");
    }

    @Override
    public byte[] encode() {
        byte[] data = new byte[0];
        Map<String, String> fields = new HashMap<>();
        fields.put("leader", leaderNode);

        return encodeWithFields(MessageType.LEADER, fields, data);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) throws IOException {
        visitor.processLeader(this, socket);
    }

    public String getLeaderNode() {
        return leaderNode;
    }

    public void setLeaderNode(String leaderNode) {
        this.leaderNode = leaderNode;
    }
}