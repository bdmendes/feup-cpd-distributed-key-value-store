package message;

public class MembershipMessage extends Message {
    public MembershipMessage(String headers, byte[] data) {

    }


    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processMembership(this);
    }
}
