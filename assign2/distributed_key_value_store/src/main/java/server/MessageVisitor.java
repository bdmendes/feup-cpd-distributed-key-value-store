package server;

import message.*;

public interface MessageVisitor {
    void processPut(PutMessage putMessage);

    void processGet(GetMessage getMessage);

    void processDelete(DeleteMessage deleteMessage);

    void processMembership(MembershipMessage membershipMessage);

    void processJoin(JoinMessage joinMessage);

    void processLeave(LeaveMessage leaveMessage);

    void process(Message message);
}
