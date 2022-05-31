package message;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GetReply extends ReplyKeyMessage {
    private byte[] value;

    public GetReply() {

    }

    public GetReply(String headers, byte[] body) {
        Map<String, String> fields = decodeFields(headers);
        setKey(fields.get("key"));
        value = body;
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
        fields.put("key", getKey());

        byte[] body;
        body = Objects.requireNonNullElseGet(value, () -> new byte[0]);

        return encodeWithFields(MessageType.GET_REPLY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) throws IOException {
        visitor.processGetReply(this, socket);
    }
}
