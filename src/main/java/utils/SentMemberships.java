package utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SentMemberships {
    private final Map<String, SentMembership> sentMemberships = new ConcurrentHashMap<>();

    public void saveSentMembership(String nodeId, int nodeMembershipCounter, int totalMembershipCounter) {
        sentMemberships.put(nodeId, new SentMembership(totalMembershipCounter, nodeMembershipCounter));
    }

    public boolean hasSentMembership(String nodeId, int nodeMembershipCounter, int totalMembershipCounter) {
        SentMembership sentMembership = sentMemberships.get(nodeId);
        return !Objects.isNull(sentMembership)
                && sentMembership.totalMembershipCounter == totalMembershipCounter
                && sentMembership.nodeMembershipCounter == nodeMembershipCounter;
    }

    private record SentMembership(int totalMembershipCounter, int nodeMembershipCounter) {
    }
}
