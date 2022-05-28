package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteMessage extends Message {
    private String key;

    public DeleteMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        this.key = fields.get("key");
    }

    public DeleteMessage() {

    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("key", key);
        byte[] data = new byte[0];
        return encodeWithFields(MessageType.DELETE, headers, data);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDelete(this, socket);
    }
}
