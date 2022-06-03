package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteReply extends KeyMessage {

    public DeleteReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
        this.setKey(fields.get("key"));
    }

    public DeleteReply() {

    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", getKey());
        return encodeWithFields(MessageType.DELETE_REPLY, fields, new byte[0]);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDeleteReply(this, socket);
    }
}
