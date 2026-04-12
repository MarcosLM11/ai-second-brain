package brain.graph;

import brain.core.model.EdgeOrigin;
import brain.core.model.EdgeType;
import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphStoreSqliteTest {

    @TempDir
    Path tempDir;

    GraphStoreSqlite store;

    @BeforeEach
    void setUp() {
        store = new GraphStoreSqlite(tempDir.resolve("brain_graph.db"));
    }

    @Test
    void schemaCreatedAutomatically() {
        var loaded = store.load();
        assertThat(loaded.vertexSet()).isEmpty();
        assertThat(loaded.edgeSet()).isEmpty();
    }

    @Test
    void roundTripPersistLoad() {
        Instant now = Instant.now();
        store.saveNode(new GraphNode("concept-a", "Concept A", NodeType.CONCEPT, -1, now, now));
        store.saveNode(new GraphNode("concept-b", "Concept B", NodeType.ENTITY, 0, now, now));

        var graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        graph.addVertex("concept-a");
        graph.addVertex("concept-b");
        var e = graph.addEdge("concept-a", "concept-b");
        graph.setEdgeWeight(e, 0.85);

        store.persist(graph, List.of(
            new GraphEdge("concept-a", "concept-b", EdgeType.RELATED_TO, EdgeOrigin.EXTRACTED, 0.85, now)
        ));

        var loaded = store.load();
        assertThat(loaded.vertexSet()).containsExactlyInAnyOrder("concept-a", "concept-b");
        assertThat(loaded.edgeSet()).hasSize(1);
        var loadedEdge = loaded.getEdge("concept-a", "concept-b");
        assertThat(loadedEdge).isNotNull();
        assertThat(loaded.getEdgeWeight(loadedEdge)).isEqualTo(0.85);
    }

    @Test
    void getNodeReturnsCorrectNode() {
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        var node = new GraphNode("entity-x", "Entity X", NodeType.ENTITY, 2, now, now);
        store.saveNode(node);

        var result = store.getNode("entity-x");
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("entity-x");
        assertThat(result.get().label()).isEqualTo("Entity X");
        assertThat(result.get().type()).isEqualTo(NodeType.ENTITY);
        assertThat(result.get().community()).isEqualTo(2);
        assertThat(result.get().createdAt()).isEqualTo(now);
        assertThat(result.get().updatedAt()).isEqualTo(now);
    }

    @Test
    void getNodeReturnsEmptyForUnknownId() {
        assertThat(store.getNode("nonexistent")).isEmpty();
    }

    @Test
    void allEdgeOriginsPersistedCorrectly() {
        var graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        graph.addVertex("a");
        graph.addVertex("b");
        graph.addVertex("c");
        graph.addVertex("d");
        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        graph.addEdge("c", "d");

        Instant now = Instant.now();
        var edges = List.of(
            new GraphEdge("a", "b", EdgeType.LINKS_TO,   EdgeOrigin.EXTRACTED,  1.0, now),
            new GraphEdge("b", "c", EdgeType.EXPLAINS,   EdgeOrigin.INFERRED,   0.7, now),
            new GraphEdge("c", "d", EdgeType.RELATED_TO, EdgeOrigin.AMBIGUOUS,  0.5, now)
        );

        store.persist(graph, edges);

        var loadedEdges = store.loadEdges();
        assertThat(loadedEdges).hasSize(3);
        assertThat(loadedEdges).extracting(GraphEdge::origin)
            .containsExactlyInAnyOrder(EdgeOrigin.EXTRACTED, EdgeOrigin.INFERRED, EdgeOrigin.AMBIGUOUS);

        var loaded = store.load();
        assertThat(loaded.vertexSet()).containsExactlyInAnyOrder("a", "b", "c", "d");
        assertThat(loaded.edgeSet()).hasSize(3);
    }

    @Test
    void persistDoesNotOverwriteExistingNodeMetadata() {
        Instant now = Instant.now();
        var node = new GraphNode("n1", "Real Label", NodeType.DECISION, 5, now, now);
        store.saveNode(node);

        var graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
        graph.addVertex("n1");
        store.persist(graph, List.of());

        var result = store.getNode("n1");
        assertThat(result).isPresent();
        assertThat(result.get().label()).isEqualTo("Real Label");
        assertThat(result.get().type()).isEqualTo(NodeType.DECISION);
    }
}
