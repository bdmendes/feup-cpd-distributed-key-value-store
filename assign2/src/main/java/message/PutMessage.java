package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PutMessage extends ReplyKeyMessage {
    protected byte[] value;

    public PutMessage() {
    }

    public PutMessage(String headers, byte[] data) {
        Map<String, String> fields = decodeFields(headers);
        setKey(fields.get("key"));
        value = data;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("key", this.getKey());
        return encodeWithFields(MessageType.PUT, fields, value);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processPut(this, socket);
    }
}
