package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutRelayMessage extends PutMessage {
    public PutRelayMessage() {
    }

    public PutRelayMessage(String headers, byte[] data) {
        super(headers, data);
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", getKey());
        return encodeWithFields(MessageType.PUT_RELAY, fields, value);
    }
}
