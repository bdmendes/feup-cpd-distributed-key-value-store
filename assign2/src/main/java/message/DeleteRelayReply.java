package message;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteRelayReply extends StatusMessage {

    public DeleteRelayReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
    }

    public DeleteRelayReply() {
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.DELETE_RELAY_REPLY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDeleteRelayReply(this, socket);
    }
}
