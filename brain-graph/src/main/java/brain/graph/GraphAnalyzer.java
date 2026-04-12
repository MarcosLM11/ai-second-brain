package brain.graph;

import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class GraphAnalyzer {

    public List<GodNode> computeGodNodes(
            Graph<String, DefaultWeightedEdge> graph,
            GraphStoreSqlite store,
            int topN) {

        if (graph.vertexSet().isEmpty()) return List.of();

        var bc = new BetweennessCentrality<>(graph);
        Map<String, Double> scores = bc.getScores();

        return scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topN)
            .map(e -> {
                GraphNode node = store.getNode(e.getKey())
                    .orElse(new GraphNode(e.getKey(), e.getKey(), NodeType.CONCEPT, -1, Instant.now(), Instant.now()));
                return new GodNode(node, e.getValue());
            })
            .toList();
    }
}
