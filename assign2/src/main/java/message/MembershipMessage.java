package message;

import java.net.Socket;

public class MembershipMessage extends Message {
    public MembershipMessage(String headers, byte[] data) {

    }


    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processMembership(this, socket);
    }
}
