package message;

import server.MessageVisitor;

import java.util.HashMap;
import java.util.Map;

public class PutMessage extends Message {
    private String key;
    private byte[] value;

    public PutMessage() {
    }

    public PutMessage(String headers, byte[] data) {
        Map<String, String> fields = Message.decodeFields(headers);
        key = fields.get("key");
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

    public byte[] getValue() {
        return value;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        return Message.encodeWithFields(MessageType.PUT, fields, value);
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processPut(this);
    }
}
