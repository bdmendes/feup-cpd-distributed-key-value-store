package message;

public class MembershipMessage extends Message {
    @Override
    protected byte[] encode(String[] fields, byte[] body) {
        return new byte[0];
    }

    @Override
    protected void decode(String message) {

    }
}
