package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteRelayMessage extends ReplyKeyMessage {
    public DeleteRelayMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        setKey(fields.get("key"));
    }

    public DeleteRelayMessage() {

    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", getKey());
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.DELETE_RELAY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDeleteRelay(this, socket);
    }
}
