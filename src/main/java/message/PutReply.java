package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutReply extends KeyMessage {

    public PutReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
        this.setKey(fields.get("key"));
    }

    public PutReply() {

    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", this.getKey());
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.PUT_REPLY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processPutReply(this, socket);
    }
}
