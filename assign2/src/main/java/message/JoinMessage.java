package message;

import java.net.Socket;

public class JoinMessage extends Message {
    public JoinMessage(String headers, byte[] data) {
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processJoin(this, socket);
    }
}
