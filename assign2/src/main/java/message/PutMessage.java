package message;

import server.MessageVisitor;

import java.util.HashMap;

public class PutMessage extends Message {
    private final String key;
    private final byte[] value;

    public PutMessage(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public PutMessage(String message) {
        var fields = decodeFields(message);
        this.value = decodeBody(message);
        this.key = fields.get("key");
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

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
