package brain.graph;

import brain.core.model.EdgeOrigin;
import brain.core.model.EdgeType;
import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for GraphTraversal using a 10-node fixture:
 *
 * n0 → n1, n2
 * n1 → n3, n4
 * n2 → n5, n6
 * n3 → n7
 * n4 → n8
 * n5 → n9
 */
class GraphTraversalTest {

    @TempDir
    Path tempDir;

    GraphStoreSqlite store;
    GraphTraversal traversal;

    @BeforeEach
    void setUp() {
        store = new GraphStoreSqlite(tempDir.resolve("brain_graph.db"));
        Instant now = Instant.now();

        // Insert 10 nodes
        for (int i = 0; i <= 9; i++) {
            store.saveNode(new GraphNode("n" + i, "Node " + i, NodeType.CONCEPT, -1, now, now));
        }

        // Insert edges
        store.insertEdges(List.of(
            new GraphEdge("n0", "n1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n0", "n2", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n1", "n3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n1", "n4", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n2", "n5", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n2", "n6", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n3", "n7", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n4", "n8", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("n5", "n9", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now)
        ));

        traversal = new GraphTraversal(store);
    }

    @Test
    void bfsHops1FromN0ReturnsN0N1N2AndDirectEdges() {
        SubGraph result = traversal.bfs("n0", 1);

        Set<String> nodeIds = result.nodes().stream().map(GraphNode::id).collect(Collectors.toSet());
        assertThat(nodeIds).containsExactlyInAnyOrder("n0", "n1", "n2");

        // Edges between n0→n1 and n0→n2 should be present
        Set<String> edgeFroms = result.edges().stream().map(GraphEdge::from).collect(Collectors.toSet());
        assertThat(edgeFroms).containsOnly("n0");
        assertThat(result.edges()).hasSize(2);
    }

    @Test
    void bfsHops2FromN0ReturnsN0ToN6ButNotN7N8N9() {
        SubGraph result = traversal.bfs("n0", 2);

        Set<String> nodeIds = result.nodes().stream().map(GraphNode::id).collect(Collectors.toSet());
        assertThat(nodeIds).containsExactlyInAnyOrder("n0", "n1", "n2", "n3", "n4", "n5", "n6");
        assertThat(nodeIds).doesNotContain("n7", "n8", "n9");

        // All edges between visited nodes should be included
        // n0→n1, n0→n2, n1→n3, n1→n4, n2→n5, n2→n6 = 6 edges
        assertThat(result.edges()).hasSize(6);
    }

    @Test
    void buildSessionContextRespectsMaxTokens() {
        String context = traversal.buildSessionContext("n0", 50);
        assertThat(context.length()).isLessThanOrEqualTo(50);
    }

    @Test
    void bfsOnNonExistentNodeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> traversal.bfs("nonexistent", 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nonexistent");
    }

    @Test
    void bfsOnLeafNodeReturnsOnlyThatNodeAndNoEdges() {
        // n9 has no outgoing edges in the fixture
        SubGraph result = traversal.bfs("n9", 2);

        Set<String> nodeIds = result.nodes().stream().map(GraphNode::id).collect(Collectors.toSet());
        assertThat(nodeIds).containsExactly("n9");
        assertThat(result.edges()).isEmpty();
    }
}
