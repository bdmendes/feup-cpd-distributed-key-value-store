package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GetMessage extends Message {
    private String key;

    public GetMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        key = fields.get("key");
    }

    public GetMessage() {

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

        return encodeWithFields(MessageType.GET, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processGet(this, socket);
    }
}
