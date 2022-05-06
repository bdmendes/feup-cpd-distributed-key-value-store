package message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GetReply extends ReplyMessage {
    private String key;
    private byte[] value;

    public GetReply() {

    }

    public GetReply(String headers, byte[] body) {
        Map<String, String> fields = decodeFields(headers);
        key = fields.get("key");
        value = body;
    }

    public byte[] getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        return encodeWithFields(MessageType.GET_REPLY, fields, value);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) throws IOException {
        visitor.processGetReply(this, socket);
    }
}
