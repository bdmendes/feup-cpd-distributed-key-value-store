package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutRelayMessage extends Message {
    private String key;
    private byte[] value;

    public PutRelayMessage() {
    }

    public PutRelayMessage(String headers, byte[] data) {
        Map<String, String> fields = decodeFields(headers);
        key = fields.get("key");
        value = data;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        return encodeWithFields(MessageType.PUT_RELAY, fields, value);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processPutRelay(this, socket);
    }
}
