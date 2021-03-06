package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.ClusterMap;
import utils.StoreUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClusterMapTest {
    private ClusterMap clusterMap1;
    private ClusterMap clusterMap2;

    @BeforeEach
    void initClusterMap() {
        this.clusterMap1 = new ClusterMap(null);
        Node node1 = new Node("190.0.0.0", 3450); // a7126f5ef2a86fc9602d160ca498b57d8a0d55464336ddad69e86f0ea104a36b
        Node node2 = new Node("200.0.0.0", 4450); // 9c4b5ce4ee530a6bd2299791f087cfbed7035a907da708d80f21397192664aab
        Node node3 = new Node("210.0.0.0", 5450); // 39c9179e5b8dea9b6dc955a028b63e22f0a5424170c0a5390caaad3d67921e5e
        clusterMap1.put(node1);
        clusterMap1.put(node2);
        clusterMap1.put(node3);
    }

    @BeforeEach
    void initClusterMap2() {
        this.clusterMap2 = new ClusterMap(null);
        Node node1 = new Node("127.0.0.1", 9002); // 12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0
        Node node2 = new Node("127.0.0.2", 9002); // 1edd62868f2767a1fff68df0a4cb3c23448e45100715768db9310b5e719536a1
        clusterMap2.put(node1);
        clusterMap2.put(node2);
    }

    @Test
    public void findResponsibleNodeForKey1() {
        String data1 = "The quick brown fox jumps over the lazy dog"; // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
        String data2 = "I am a good boy!"; // 3a9d52586bd59b25cf024a45005041038238e224ab783ef49f9c082a059aab1b
        assertEquals(clusterMap1.getNodeResponsibleForHash(StoreUtils.sha256(data1.getBytes(StandardCharsets.UTF_8))),
                new Node("210.0.0.0", 5450));
        assertEquals(clusterMap1.getNodeResponsibleForHash(StoreUtils.sha256(data2.getBytes(StandardCharsets.UTF_8))),
                new Node("200.0.0.0", 4450));
        clusterMap1.removeId("200.0.0.0");
        assertEquals(clusterMap1.getNodeResponsibleForHash(StoreUtils.sha256(data2.getBytes(StandardCharsets.UTF_8))),
                new Node("190.0.0.0", 3450));
    }

    @Test
    public void findResponsibleNodeForKey2() {
        String data1 = "The quick brown fox jumps over the lazy dog"; // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
        assertEquals(clusterMap2.getNodeResponsibleForHash(StoreUtils.sha256(data1.getBytes(StandardCharsets.UTF_8))), new Node("127.0.0.1", 9002));
    }

    @Test
    public void findSuccessorNode1() {
        assertEquals(new Node("200.0.0.0", 4450), clusterMap1.getNodeSuccessor(new Node("210.0.0.0", 5450)));
        assertEquals(new Node("190.0.0.0", 3450), clusterMap1.getNodeSuccessor(new Node("200.0.0.0", 4450)));
        assertEquals(new Node("210.0.0.0", 5450), clusterMap1.getNodeSuccessor(new Node("190.0.0.0", 3450)));
    }

    @Test
    public void findSuccessorNode2() {
        assertEquals(new Node("127.0.0.1", 9002), clusterMap2.getNodeSuccessor(new Node("127.0.0.2", 9002)));
        assertEquals(new Node("127.0.0.2", 9002), clusterMap2.getNodeSuccessor(new Node("127.0.0.1", 9002)));
    }


    @Test
    public void responsibleNodesCountMoreOrEqual() {
        String data = "The quick brown fox jumps over the lazy dog"; // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
        String hash = StoreUtils.sha256(data.getBytes(StandardCharsets.UTF_8));

        Node node1 = new Node("127.0.0.1", 9002); // 12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0
        Node node2 = new Node("127.0.0.2", 9002);

        assertEquals(List.of(node2), clusterMap2.getReplicationNodes(node1, 2));
        assertEquals(List.of(node1), clusterMap2.getReplicationNodes(node2, 3));
    }

    @Test
    public void responsibleNodesCountLess() {
        String data = "The quick brown fox jumps over the lazy dog"; // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
        String hash = StoreUtils.sha256(data.getBytes(StandardCharsets.UTF_8));

        Node node = new Node("127.0.0.1", 9002); // 12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0

        assertEquals(0, clusterMap2.getReplicationNodes(node, 1).size());
    }
}
