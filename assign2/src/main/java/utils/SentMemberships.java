package utils;

import message.JoinMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SentMemberships {
    private record SentMembership(int totalMembershipCounter, int nodeMembershipCounter) {
    }

    private final Map<String, SentMembership> sentMemberships = new HashMap<>();

    public void saveSentMembership(String nodeId, int nodeMembershipCounter, int totalMembershipCounter) {
        sentMemberships.put(nodeId, new SentMembership(totalMembershipCounter, nodeMembershipCounter));
    }

    public boolean hasSentMembership(String nodeId, int nodeMembershipCounter, int totalMembershipCounter) {
        SentMembership sentMembership = sentMemberships.get(nodeId);

        if (Objects.isNull(sentMembership)) {
            return false;
        }

        return sentMembership.totalMembershipCounter == totalMembershipCounter && sentMembership.nodeMembershipCounter == nodeMembershipCounter;
    }
}
