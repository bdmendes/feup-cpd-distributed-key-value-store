package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class JoinMessage extends Message {
    private String nodeId;
    private int counter;
    private int connectionPort;
    private int port;

    public JoinMessage() {
    }

    public JoinMessage(String header) {
        Map<String, String> fields = decodeFields(header);
        this.nodeId = fields.get("nodeId");
        this.counter = Integer.parseInt(fields.get("counter"));
        this.connectionPort = Integer.parseInt(fields.get("connectionPort"));
        this.port = Integer.parseInt(fields.get("port"));
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getConnectionPort() {
        return connectionPort;
    }

    public void setConnectionPort(int connectionPort) {
        this.connectionPort = connectionPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public byte[] encode() {
        Map<String, String> fields = new HashMap<>();
        fields.put("nodeId", this.nodeId);
        fields.put("counter", Integer.toString(counter));
        fields.put("connectionPort", Integer.toString(connectionPort));
        fields.put("port", Integer.toString(port));
        return encodeWithFields(MessageType.JOIN, fields, new byte[]{});
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processJoin(this);
    }
}
