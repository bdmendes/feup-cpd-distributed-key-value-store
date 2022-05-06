package message;

import java.util.HashMap;
import java.util.Map;

public class GetMessage extends Message {
    private String key;

    public GetMessage(String headers) {
        Map<String, String> fields = Message.decodeFields(headers);
        key = fields.get("key");
    }

    public GetMessage() {

    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        byte[] body = new byte[0];

        return Message.encodeWithFields(MessageType.GET, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processGet(this);
    }
}
