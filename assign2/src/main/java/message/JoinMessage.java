package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class JoinMessage extends Message {
    private String nodeId;
    private int counter;

    public JoinMessage() {}

    public JoinMessage(String header) {
        Map<String, String> fields = decodeFields(header);
        this.nodeId = fields.get("nodeId");
        this.counter = Integer.parseInt(fields.get("counter"));
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getCounter() {
        return counter;
    }

    @Override
    public byte[] encode() {
        Map<String, String> fields = new HashMap<>();
        fields.put("nodeId", this.nodeId);
        fields.put("counter", Integer.toString(counter));
        return encodeWithFields(MessageType.JOIN, fields, new byte[]{});
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processJoin(this, socket);
    }
}
