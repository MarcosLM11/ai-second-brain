package brain.graph;

import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import org.jgrapht.Graph;
import org.jgrapht.alg.clustering.GirvanNewmanClustering;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.time.Instant;
import java.util.HashMap;
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

    /**
     * Applies Girvan-Newman clustering to identify {@code communityCount} communities.
     *
     * <p>Returns a map of {@code nodeId → communityId} (0-indexed) and persists
     * the community assignment back to SQLite for every node found in the store.
     */
    public Map<String, Integer> detectCommunities(
            Graph<String, DefaultWeightedEdge> graph,
            GraphStoreSqlite store,
            int communityCount) {

        if (graph.vertexSet().isEmpty()) return Map.of();

        int k = Math.min(communityCount, graph.vertexSet().size());
        var clusters = new GirvanNewmanClustering<>(graph, k).getClustering().getClusters();

        Map<String, Integer> communityMap = new HashMap<>();
        for (int i = 0; i < clusters.size(); i++) {
            for (String nodeId : clusters.get(i)) {
                communityMap.put(nodeId, i);
            }
        }

        // Persist community assignment back to SQLite
        for (var entry : communityMap.entrySet()) {
            store.getNode(entry.getKey()).ifPresent(node ->
                store.saveNode(new GraphNode(
                    node.id(), node.label(), node.type(),
                    entry.getValue(), node.createdAt(), node.updatedAt()
                ))
            );
        }

        return communityMap;
    }
}
