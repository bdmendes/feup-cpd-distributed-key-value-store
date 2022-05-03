package message;

import server.MessageVisitor;

public class JoinMessage extends Message{
    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    protected void decode(String message) {

    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processJoin(this);
    }
}
