package message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteReply extends ReplyMessage {
    private String key;

    public DeleteReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
        this.key = fields.get("key");
    }

    public DeleteReply() {

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
        return encodeWithFields(MessageType.DELETE_REPLY, fields, new byte[0]);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) throws IOException {
        visitor.processDeleteReply(this, socket);
    }
}
