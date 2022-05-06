package message;

import server.MessageVisitor;

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

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processGet(this);
    }
}
