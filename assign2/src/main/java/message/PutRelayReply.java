package message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutRelayReply extends ReplyKeyMessage {
    private String key;
    public PutRelayReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
        this.key = fields.get("key");
    }

    public PutRelayReply() {

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", key);
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.PUT_RELAY_REPLY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) throws IOException {
        visitor.processPutRelayReply(this, socket);
    }
}
