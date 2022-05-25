package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ClusterMap;
import utils.StoreUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClusterMapTest {
    private ClusterMap clusterMap;

    @BeforeEach
    void initClusterMap() {
        this.clusterMap = new ClusterMap(null);
        Node node1 = new Node("190.0.0.0", 3450); // a7126f5ef2a86fc9602d160ca498b57d8a0d55464336ddad69e86f0ea104a36b
        Node node2 = new Node("200.0.0.0", 4450); // 9c4b5ce4ee530a6bd2299791f087cfbed7035a907da708d80f21397192664aab
        Node node3 = new Node("210.0.0.0", 5450); // 39c9179e5b8dea9b6dc955a028b63e22f0a5424170c0a5390caaad3d67921e5e
        clusterMap.put(node1);
        clusterMap.put(node2);
        clusterMap.put(node3);
    }

    @Test
    public void findResponsibleNodeForKey() {
        String data1 = "The quick brown fox jumps over the lazy dog"; // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
        String data2 = "I am a good boy!"; // 3a9d52586bd59b25cf024a45005041038238e224ab783ef49f9c082a059aab1b
        assertEquals(clusterMap.getNodeResponsibleForHash(StoreUtils.sha256(data1.getBytes(StandardCharsets.UTF_8))),
                new Node("210.0.0.0", 5450));
        assertEquals(clusterMap.getNodeResponsibleForHash(StoreUtils.sha256(data2.getBytes(StandardCharsets.UTF_8))),
                new Node("200.0.0.0", 4450));
        clusterMap.removeId("200.0.0.0");
        assertEquals(clusterMap.getNodeResponsibleForHash(StoreUtils.sha256(data2.getBytes(StandardCharsets.UTF_8))),
                new Node("190.0.0.0", 3450));
    }

    @Test
    public void findSuccessorNode() {
        assertEquals(new Node("200.0.0.0", 4450), clusterMap.getNodeSuccessor(new Node("210.0.0.0", 5450)));
        assertEquals(new Node("190.0.0.0", 3450), clusterMap.getNodeSuccessor(new Node("200.0.0.0", 4450)));
        assertEquals(new Node("210.0.0.0", 5450), clusterMap.getNodeSuccessor(new Node("190.0.0.0", 3450)));
    }
}
