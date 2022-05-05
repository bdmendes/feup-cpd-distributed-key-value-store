package message;

import server.MessageVisitor;

public class DeleteMessage extends Message {
    public DeleteMessage(String headers, byte[] data) {

    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processDelete(this);
    }
}
