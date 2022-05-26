package utils;

import message.MessageConstants;
import server.Node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClusterMap {
    private final Map<String, Node> clusterNodes;
    private final String filePath;

    public ClusterMap(String filePath) {
        this.clusterNodes = Collections.synchronizedSortedMap(new TreeMap<>());
        this.filePath = filePath;
        this.readFromFile();
    }

    public Set<Node> getNodes() {
        return new HashSet<>(clusterNodes.values());
    }

    public void put(Node node) {
        clusterNodes.put(StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8)), node);
        this.writeToFile();
    }

    public void remove(Node node) {
        this.removeHash(StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8)));
    }

    public void removeHash(String hash) {
        clusterNodes.remove(hash);
        this.writeToFile();
    }

    public void removeId(String id) {
        this.removeHash(StoreUtils.sha256(id.getBytes(StandardCharsets.UTF_8)));
    }

    public void clear() {
        clusterNodes.clear();
        this.writeToFile();
    }

    public Node getNodeSuccessor(Node node) {
        String nodeHash = StoreUtils.sha256(node.id().getBytes(StandardCharsets.UTF_8));
        return this.getNodeSuccessor(nodeHash);
    }

    public Node getNodeSuccessorById(String nodeId) {
        String nodeHash = StoreUtils.sha256(nodeId.getBytes(StandardCharsets.UTF_8));
        return this.getNodeSuccessor(nodeHash);
    }

    public Node getNodeResponsibleForHash(String hash) {
        return this.getNodeSuccessor(hash);
    }

    private Node getNodeSuccessor(String hash) {
        if (clusterNodes.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, Node> entry : clusterNodes.entrySet()) {
            String currentNodeHash = entry.getKey();
            Node currentNode = entry.getValue();
            if (currentNodeHash.compareTo(hash) > 0) {
                return currentNode;
            }
        }

        return clusterNodes.values().iterator().next();
    }

    private void readFromFile() {
        if (filePath == null) {
            return;
        }
        Scanner scanner;
        try {
            scanner = new Scanner(new File(filePath));
        } catch (FileNotFoundException e) {
            return;
        }
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split(" ");
            String nodeId = line[0];
            int port = Integer.parseInt(line[1]);
            this.put(new Node(nodeId, port));
        }
        scanner.close();
    }

    private void writeToFile() {
        if (filePath == null) {
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        clusterNodes.forEach((key, value) -> stringBuilder.append(value.id())
                .append(" ")
                .append(value.port())
                .append(MessageConstants.END_OF_LINE));
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
