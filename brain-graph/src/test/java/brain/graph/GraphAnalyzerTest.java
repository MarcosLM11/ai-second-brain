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
}
