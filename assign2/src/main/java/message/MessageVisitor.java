package message;

import java.io.IOException;
import java.net.Socket;

public interface MessageVisitor {
    void processPut(PutMessage putMessage, Socket socket);

    void processGet(GetMessage getMessage, Socket socket);

    void processDelete(DeleteMessage deleteMessage, Socket socket);

    void processMembership(MembershipMessage membershipMessage, Socket socket);

    void processJoin(JoinMessage joinMessage, Socket socket);

    void processGetReply(GetReply getReply, Socket socket) throws IOException;

    void processPutReply(PutReply putReply, Socket socket) throws IOException;

    void processDeleteReply(DeleteReply deleteReply, Socket socket) throws IOException;

    void process(Message message, Socket socket) throws IOException;
}
