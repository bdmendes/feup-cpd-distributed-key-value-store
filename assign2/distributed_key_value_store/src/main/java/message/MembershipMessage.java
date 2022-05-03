package message;

public class MembershipMessage extends Message {
    public MembershipMessage(MessageType messageType) {
        super(messageType);
    }

    @Override
    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }
}
