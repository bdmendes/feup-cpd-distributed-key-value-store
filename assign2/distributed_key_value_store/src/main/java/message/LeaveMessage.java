package message;

import server.MessageVisitor;

public class LeaveMessage extends Message {
    public LeaveMessage(String message) {

    }

    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processLeave(this);
    }
}
