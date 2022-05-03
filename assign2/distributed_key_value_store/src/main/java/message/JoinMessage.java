package message;

import server.MessageVisitor;

public class JoinMessage extends Message {
    public JoinMessage(String message) {
    }

    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processJoin(this);
    }
}
