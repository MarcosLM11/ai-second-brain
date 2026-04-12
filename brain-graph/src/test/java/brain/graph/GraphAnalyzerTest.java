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
import static org.assertj.core.api.Assertions.assertThat;

class GraphAnalyzerTest {

    @TempDir
    Path tempDir;

    GraphStoreSqlite store;
    GraphAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        store = new GraphStoreSqlite(tempDir.resolve("brain_graph.db"));
        analyzer = new GraphAnalyzer();

        Instant now = Instant.now();
        store.saveNode(new GraphNode("hub", "Hub", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("a", "A", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("b", "B", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("c", "C", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("d", "D", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("e", "E", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("f", "F", NodeType.CONCEPT, -1, now, now));
        store.insertEdges(List.of(
            new GraphEdge("a", "hub", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("b", "hub", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("c", "hub", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("d", "hub", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("hub", "e", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("hub", "f", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now)
        ));
    }

    @Test
    void hubAparecesPrimero() {
        var graph = store.load();
        var result = analyzer.computeGodNodes(graph, store, 3);
        assertThat(result.get(0).node().id()).isEqualTo("hub");
    }

    @Test
    void topNRespetaElLimite() {
        var graph = store.load();
        var result = analyzer.computeGodNodes(graph, store, 3);
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    void scoreAccesibleYPositivo() {
        var graph = store.load();
        var result = analyzer.computeGodNodes(graph, store, 3);
        assertThat(result.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void grafoVacioRetornaListaVacia() {
        var emptyStore = new GraphStoreSqlite(tempDir.resolve("empty_graph.db"));
        var emptyGraph = emptyStore.load();
        var result = analyzer.computeGodNodes(emptyGraph, emptyStore, 10);
        assertThat(result).isEmpty();
    }

    @Test
    void topNMayorQueNodosDisponibles() {
        var graph = store.load();
        var result = analyzer.computeGodNodes(graph, store, 100);
        assertThat(result.size()).isLessThanOrEqualTo(7);
    }

    // ---- detectCommunities ----

    /**
     * Fixture: two triangles (p1-p2-p3) and (q1-q2-q3) connected by a single bridge p3↔q1.
     * Girvan-Newman with k=2 should cut the bridge and yield two distinct communities.
     */
    private GraphStoreSqlite buildTwoClusterStore(Path dir) {
        var s = new GraphStoreSqlite(dir.resolve("two_cluster.db"));
        Instant now = Instant.now();
        for (String id : List.of("p1", "p2", "p3", "q1", "q2", "q3")) {
            s.saveNode(new GraphNode(id, id, NodeType.CONCEPT, -1, now, now));
        }
        s.insertEdges(List.of(
            // cluster P (bidirectional triangle)
            new GraphEdge("p1", "p2", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("p2", "p1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("p2", "p3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("p3", "p2", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("p1", "p3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("p3", "p1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            // cluster Q (bidirectional triangle)
            new GraphEdge("q1", "q2", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q2", "q1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q2", "q3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q3", "q2", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q1", "q3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q3", "q1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            // bridge (bidirectional single link)
            new GraphEdge("p3", "q1", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now),
            new GraphEdge("q1", "p3", EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, now)
        ));
        return s;
    }

    @Test
    void dosClusteresProducenDosComunidadesDistintas() {
        var s = buildTwoClusterStore(tempDir);
        var graph = s.load();

        var communityMap = analyzer.detectCommunities(graph, s, 2);

        // All 6 nodes must be assigned
        assertThat(communityMap.keySet()).containsExactlyInAnyOrder("p1", "p2", "p3", "q1", "q2", "q3");

        // Nodes within each cluster share the same community id
        int communityP = communityMap.get("p1");
        assertThat(communityMap.get("p2")).isEqualTo(communityP);
        assertThat(communityMap.get("p3")).isEqualTo(communityP);

        int communityQ = communityMap.get("q1");
        assertThat(communityMap.get("q2")).isEqualTo(communityQ);
        assertThat(communityMap.get("q3")).isEqualTo(communityQ);

        // The two clusters are in different communities
        assertThat(communityP).isNotEqualTo(communityQ);
    }

    @Test
    void todosLosNodosTienenCommunityIdAsignado() {
        var s = buildTwoClusterStore(tempDir);
        var graph = s.load();

        var communityMap = analyzer.detectCommunities(graph, s, 2);

        assertThat(communityMap).hasSize(6);
        assertThat(communityMap.values()).doesNotContainNull();
    }

    @Test
    void numeroDeComunidadesRespetaParametro() {
        var s = buildTwoClusterStore(tempDir);
        var graph = s.load();

        var communityMap = analyzer.detectCommunities(graph, s, 2);

        long distinctCommunities = communityMap.values().stream().distinct().count();
        assertThat(distinctCommunities).isEqualTo(2);
    }

    @Test
    void sqliteActualizadoConCommunityId() {
        var s = buildTwoClusterStore(tempDir);
        var graph = s.load();

        analyzer.detectCommunities(graph, s, 2);

        // All nodes should have community != -1 after detection
        for (String id : List.of("p1", "p2", "p3", "q1", "q2", "q3")) {
            assertThat(s.getNode(id))
                .isPresent()
                .get()
                .extracting(n -> n.community())
                .isNotEqualTo(-1);
        }
    }
}
