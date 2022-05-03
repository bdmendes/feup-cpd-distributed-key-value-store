package message;

import server.MessageVisitor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class PutMessage extends Message{
    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    protected byte[] encode(String key, byte[] value) {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        return Message.encodeWithFields(MessageType.PUT, fields, value);
    }

    @Override
    protected void decode(String message) {
        var fields = decodeFields(message);
        var body = decodeBody(message);
        this.value = new String(body, StandardCharsets.UTF_8);
        this.key = fields.get("key");
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processPut(this);
    }
}
