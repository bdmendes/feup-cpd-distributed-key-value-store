package message;

import java.net.Socket;

public class LeaveMessage extends Message {
    public LeaveMessage(String headers, byte[] data) {

    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processLeave(this, socket);
    }
}
