package message;

import server.MessageVisitor;

public class GetMessage extends Message {
    public GetMessage(String message) {
    }

    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor) {
        visitor.processGet(this);
    }
}
