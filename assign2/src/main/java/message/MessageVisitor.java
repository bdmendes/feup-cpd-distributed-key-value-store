package message;

import java.io.IOException;
import java.net.Socket;

public interface MessageVisitor {
    default void processPut(PutMessage putMessage, Socket socket) {

    }

    default void processPutRelay(PutRelayMessage putRelayMessage, Socket socket) {

    }

    default void processDeleteRelay(DeleteRelayMessage deleteRelayMessage, Socket socket) {

    }

    default void processGet(GetMessage getMessage, Socket socket) {

    }

    default void processDelete(DeleteMessage deleteMessage, Socket socket) {

    }

    default void processMembership(MembershipMessage membershipMessage) {

    }

    default void processJoin(JoinMessage joinMessage) {

    }

    default void processGetReply(GetReply getReply, Socket socket) {

    }

    default void processPutReply(PutReply putReply, Socket socket) {

    }

    default void processDeleteReply(DeleteReply deleteReply, Socket socket) {

    }

    void process(Message message, Socket socket) throws IOException;

    default void processElection(ElectionMessage electionMessage) {

    }

    default void processLeader(LeaderMessage leaderMessage) {

    }

    default void processPutRelayReply(PutRelayReply putRelayReply, Socket socket) {

    }

    default void processGetRelay(GetRelayMessage getRelayMessage, Socket socket) {

    }
}
