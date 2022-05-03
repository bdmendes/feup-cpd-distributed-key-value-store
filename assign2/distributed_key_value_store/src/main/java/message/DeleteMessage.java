package message;

import server.MessageVisitor;

public class DeleteMessage extends Message {
    public DeleteMessage(String message) {

    }

    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processDelete(this);
    }
}
