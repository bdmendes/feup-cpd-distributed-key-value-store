package message;

import java.io.IOException;
import java.net.Socket;

public interface MessageVisitor {
    default void processPut(PutMessage putMessage, Socket socket) {

    }

    default void processGet(GetMessage getMessage, Socket socket) {

    }

    default void processDelete(DeleteMessage deleteMessage, Socket socket) {

    }

    default void processMembership(MembershipMessage membershipMessage, Socket socket) {

    }

    default void processJoin(JoinMessage joinMessage, Socket socket) {

    }

    default void processGetReply(GetReply getReply, Socket socket) {

    }

    default void processPutReply(PutReply putReply, Socket socket) {

    }

    default void processDeleteReply(DeleteReply deleteReply, Socket socket) {

    }

    void process(Message message, Socket socket) throws IOException;

    default void processElection(ElectionMessage electionMessage, Socket socket) {

    }

    default void processLeader(LeaderMessage leaderMessage, Socket socket) {

    }

}
