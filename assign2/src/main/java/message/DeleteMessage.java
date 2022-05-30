package message;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class DeleteMessage extends ReplyKeyMessage {
    public DeleteMessage(String headers) {
        Map<String, String> fields = decodeFields(headers);
        setKey(fields.get("key"));
    }

    public DeleteMessage() {

    }

    @Override
    public byte[] encode() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("key", getKey());
        byte[] data = new byte[0];
        return encodeWithFields(MessageType.DELETE, headers, data);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDelete(this, socket);
    }
}
