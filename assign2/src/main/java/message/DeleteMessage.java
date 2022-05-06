package message;

import server.MessageVisitor;

import java.util.HashMap;
import java.util.Map;

public class DeleteMessage extends Message {
    private String key;

    public DeleteMessage(String headers) {
        Map<String, String> fields = Message.decodeFields(headers);
        this.key = fields.get("key");
    }

    public DeleteMessage() {

    }

    public String getKey() {
        return key;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("key", key);
        byte[] data = new byte[0];
        return Message.encodeWithFields(MessageType.DELETE, headers, data);
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processDelete(this);
    }

    public void setKey(String key) {
        this.key = key;
    }
}
