package message;

import server.MessageVisitor;

public class MembershipMessage extends Message {
    public MembershipMessage(String message) {

    }

    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processMembership(this);
    }
}
