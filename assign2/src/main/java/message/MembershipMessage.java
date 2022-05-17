package message;

import server.Node;
import utils.MembershipLog;

import java.net.Socket;
import java.util.*;

public class MembershipMessage extends Message {
    private final Set<Node> nodes;
    private final Map<String, Integer> membershipLog;

    public MembershipMessage(Set<Node> nodes, Map<String, Integer> membershipLog) {
        this.nodes = nodes;
        this.membershipLog = membershipLog;
    }

    public MembershipMessage(String headers, byte[] data) {
        this.nodes = new HashSet<>();
        membershipLog = MembershipLog.generateMembershipLog();
        Map<String, String> fields = decodeFields(headers);
        String nodeString = fields.get("nodes");
        List<String> nodesRaw = List.of(nodeString.split(","));

        nodesRaw.forEach(n -> {
            String[] parts = n.split("/");
            nodes.add(new Node(parts[0], Integer.parseInt(parts[1])));
        });
        MembershipLog.readMembershipLogFromData(membershipLog, data);
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node node){
        this.nodes.add(node);
    }

    public Map<String, Integer> getMembershipLog() {
        return membershipLog;
    }

    @Override
    public byte[] encode() {
        byte[] data = MembershipLog.writeMembershipLogToData(membershipLog);
        Map<String, String> fields = new HashMap<>();

        List<String> nodeList = nodes.stream().map(n -> n.id() + "/" + n.port()).toList();
        String nodeString = String.join(",", nodeList);
        fields.put("nodes", nodeString);

        return encodeWithFields(MessageType.MEMBERSHIP, fields, data);
    }

    @Override
    public void accept(MessageVisitor visitor, Socket socket) {
        visitor.processMembership(this, socket);
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().equals(this.getClass())) {
            return false;
        }
        MembershipMessage other = (MembershipMessage) obj;
        return membershipLog.equals(other.getMembershipLog()) && nodes.equals(other.getNodes());
    }
}
