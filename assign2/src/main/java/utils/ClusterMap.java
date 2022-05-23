package utils;

import server.Node;
import utils.MembershipLog;
import utils.StoreUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClusterMap {
    private final Map<String, Node> clusterNodes;
    private final String filePath;

    public ClusterMap(String filePath) {
        this.clusterNodes = Collections.synchronizedSortedMap(new TreeMap<>());
        this.filePath = filePath;
    }

    public Set<Node> getNodes() {
        return new HashSet<>(clusterNodes.values());
    }

    public void add(Node node) {
        clusterNodes.put(StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8)), node);
    }

    public void remove(Node node) {
        this.removeHash(StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8)));
    }

    public void removeHash(String hash) {
        clusterNodes.remove(hash);
    }

    public void removeId(String id) {
        this.removeHash(StoreUtils.sha256(id.getBytes(StandardCharsets.UTF_8)));
    }

    public void clear() {
        clusterNodes.clear();
    }

    public Node getNodeSuccessor(Node node) {
        String nodeHash = StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8));
        return this.getNodeSuccessor(nodeHash);
    }

    public Node getNodeSuccessorById(String nodeId) {
        String nodeHash = StoreUtils.sha256(nodeId.getBytes(StandardCharsets.UTF_8));
        return this.getNodeSuccessor(nodeHash);
    }

    public Node getNodeResponsibleForHash(String hash){
        return this.getNodeSuccessor(hash);
    }

    private Node getNodeSuccessor(String hash) {
        if (clusterNodes.isEmpty()){
            return null;
        }
        for (Map.Entry<String,Node> entry : clusterNodes.entrySet()) {
            String currentNodeHash = entry.getKey();
            Node currentNode = entry.getValue();
            if (currentNodeHash.compareTo(hash) > 0){
                return currentNode;
            }
        }
        return clusterNodes.entrySet().iterator().next().getValue();
    }
}