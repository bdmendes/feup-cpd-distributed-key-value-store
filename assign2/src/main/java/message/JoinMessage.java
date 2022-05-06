package message;

public class JoinMessage extends Message {
    public JoinMessage(String headers, byte[] data) {
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processJoin(this);
    }
}
