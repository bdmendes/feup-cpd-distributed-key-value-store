package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteRelayMessage extends ReplyKeyMessage {
    private boolean transference = false;

    public DeleteRelayMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        transference = Boolean.parseBoolean(fields.get("transference"));
        setKey(fields.get("key"));
    }

    public DeleteRelayMessage() {

    }

    public void setTransference(boolean transference) {
        this.transference = transference;
    }

    public boolean isTransference() {
        return this.transference;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", getKey());
        fields.put("transference", Boolean.valueOf(transference).toString());
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.DELETE_RELAY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDeleteRelay(this, socket);
    }
}
