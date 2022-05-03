package message;

import server.MessageVisitor;

public class DeleteMessage extends Message{
    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    protected void decode(String message) {

    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processDelete(this);
    }
}
