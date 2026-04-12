package brain.graph;

import brain.core.model.Community;
import brain.core.model.GraphAnalysis;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import brain.core.model.SurpriseEdge;
import org.jgrapht.Graph;
import org.jgrapht.alg.clustering.GirvanNewmanClustering;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.graph.DefaultWeightedEdge;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * Full analysis: god nodes, communities, surprise edges, and an expertise map.
     *
     * @throws IllegalStateException if the graph has not been built yet
     */
    public GraphAnalysis analyze(GraphStoreSqlite store, int topN, int communityCount, double minConf) {
        var graph = store.load();

        if (graph.vertexSet().isEmpty()) {
            throw new IllegalStateException("Graph is empty. Run graph_build first.");
        }

        List<GodNode> godNodesList = computeGodNodes(graph, store, topN);
        Map<String, Integer> communityMap = detectCommunities(graph, store, communityCount);

        Map<Integer, Set<String>> grouped = new HashMap<>();
        communityMap.forEach((nodeId, cId) -> grouped.computeIfAbsent(cId, k -> new HashSet<>()).add(nodeId));
        List<Community> communities = grouped.entrySet().stream()
            .map(e -> new Community(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingInt(Community::id))
            .toList();

        List<SurpriseEdge> surprises = findSurprises(graph, communityMap, minConf);

        var bc = new BetweennessCentrality<>(graph);
        Map<String, Double> expertiseMap = bc.getScores();

        return new GraphAnalysis(
            godNodesList.stream().map(GodNode::node).toList(),
            communities,
            surprises,
            expertiseMap
        );
    }

    /**
     * Finds surprise edges: cross-community aristas with confidence ≥ {@code minConfidence},
     * sorted by weight descending.
     *
     * @param graph       the JGraphT graph (edge weights = confidence)
     * @param communities map of nodeId → communityId, as returned by {@link #detectCommunities}
     * @param minConfidence minimum confidence threshold (e.g. 0.7)
     */
    public List<SurpriseEdge> findSurprises(
            Graph<String, DefaultWeightedEdge> graph,
            Map<String, Integer> communities,
            double minConfidence) {

        return graph.edgeSet().stream()
            .filter(edge -> {
                String from = graph.getEdgeSource(edge);
                String to   = graph.getEdgeTarget(edge);
                Integer cFrom = communities.get(from);
                Integer cTo   = communities.get(to);
                if (cFrom == null || cTo == null) return false;
                return !cFrom.equals(cTo) && graph.getEdgeWeight(edge) >= minConfidence;
            })
            .map(edge -> new SurpriseEdge(
                graph.getEdgeSource(edge),
                graph.getEdgeTarget(edge),
                graph.getEdgeWeight(edge)
            ))
            .sorted(Comparator.comparingDouble(SurpriseEdge::weight).reversed())
            .toList();
    }
}
