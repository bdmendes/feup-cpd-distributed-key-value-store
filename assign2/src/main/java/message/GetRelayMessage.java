package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GetRelayMessage extends KeyMessage {

    public GetRelayMessage() {
    }

    public GetRelayMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        setKey(fields.get("key"));
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", getKey());
        return encodeWithFields(MessageType.GET_RELAY, fields, new byte[0]);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processGetRelay(this, socket);
    }
}
