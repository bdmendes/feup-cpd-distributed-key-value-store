package message;

import server.MessageVisitor;

public class GetMessage extends Message {
    public GetMessage(String message) {
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processGet(this);
    }
}
