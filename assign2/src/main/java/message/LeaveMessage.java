package message;

import server.MessageVisitor;

public class LeaveMessage extends Message {
    public LeaveMessage(String message) {

    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processLeave(this);
    }
}
