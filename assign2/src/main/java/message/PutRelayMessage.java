package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutRelayMessage extends Message {
    private String key;
    private String target;
    private byte[] value;

    public PutRelayMessage() {}

    public PutRelayMessage(String headers, byte[] data) {
        Map<String, String> fields = decodeFields(headers);
        key = fields.get("key");
        target = fields.get("target");
        value = data;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        fields.put("target", target);
        return encodeWithFields(MessageType.PUT_RELAY, fields, value);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processPutRelay(this, socket);
    }
}
