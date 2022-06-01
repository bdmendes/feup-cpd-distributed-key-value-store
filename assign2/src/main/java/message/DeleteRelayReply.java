package message;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteRelayReply extends ReplyMessage {
    final List<String> successfulHashes;

    public DeleteRelayReply(String headers) {
        Map<String, String> fields = decodeFields(headers);
        String hashes = fields.get("hashes");
        successfulHashes = List.of(hashes.split(","));
    }

    public DeleteRelayReply() {
        successfulHashes = new ArrayList<>();
    }

    public void reportSuccess(String hash) {
        successfulHashes.add(hash);
    }

    public List<String> getSuccessfulHashes() {
        return successfulHashes;
    }

    @Override
    public byte[] encode() {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("hashes", String.join(",", successfulHashes));
        byte[] body = new byte[0];

        return encodeWithFields(MessageType.DELETE_RELAY_REPLY, fields, body);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processDeleteRelayReply(this, socket);
    }
}
