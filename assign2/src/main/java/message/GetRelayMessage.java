package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GetRelayMessage extends Message {
    private String key;
    private String target;

    public GetRelayMessage() {
    }

    public GetRelayMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        key = fields.get("key");
        target = fields.get("target");
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
        fields.put("target", target);
        return encodeWithFields(MessageType.GET_RELAY, fields, new byte[0]);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processGetRelay(this, socket);
    }
}
