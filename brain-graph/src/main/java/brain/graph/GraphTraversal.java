package brain.graph;

import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import org.jgrapht.graph.DefaultWeightedEdge;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class GraphTraversal {

    private final GraphStoreSqlite graph;

    public GraphTraversal(GraphStoreSqlite graph) {
        this.graph = graph;
    }

    /**
     * BFS from {@code nodeId} up to {@code hops} levels of depth.
     *
     * @param nodeId starting node ID
     * @param hops   number of hops to traverse (must be >= 1)
     * @return SubGraph containing all visited nodes and edges between them
     * @throws IllegalArgumentException if the starting node does not exist in the graph
     */
    public SubGraph bfs(String nodeId, int hops) {
        var jgraph = graph.load();

        if (!jgraph.containsVertex(nodeId)) {
            throw new IllegalArgumentException("Node not found in graph: " + nodeId);
        }

        // BFS with hop-distance tracking
        Map<String, Integer> distance = new LinkedHashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        distance.put(nodeId, 0);
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = distance.get(current);
            if (d >= hops) continue;
            for (DefaultWeightedEdge edge : jgraph.outgoingEdgesOf(current)) {
                String neighbor = jgraph.getEdgeTarget(edge);
                if (!distance.containsKey(neighbor)) {
                    distance.put(neighbor, d + 1);
                    queue.add(neighbor);
                }
            }
        }

        Set<String> visitedIds = distance.keySet();

        // Resolve GraphNode metadata for each visited ID
        Instant fallbackTime = Instant.now();
        List<GraphNode> nodes = new ArrayList<>(visitedIds.size());
        for (String id : visitedIds) {
            GraphNode node = graph.getNode(id).orElseGet(() ->
                new GraphNode(id, id, NodeType.CONCEPT, -1, fallbackTime, fallbackTime)
            );
            nodes.add(node);
        }

        // Filter edges to only those between visited nodes
        List<GraphEdge> edges = graph.loadEdges().stream()
            .filter(e -> visitedIds.contains(e.from()) && visitedIds.contains(e.to()))
            .toList();

        return new SubGraph(nodes, edges);
    }

    /**
     * BFS from {@code projectNodeId} with hops=2, formatted as markdown.
     * Returns empty string if the node does not exist.
     *
     * @param projectNodeId starting node ID
     * @param maxTokens     maximum character length of the result (truncates if exceeded)
     * @return markdown-formatted subgraph context, or empty string if node not found
     */
    public String buildSessionContext(String projectNodeId, int maxTokens) {
        var jgraph = graph.load();
        if (!jgraph.containsVertex(projectNodeId)) {
            return "";
        }

        SubGraph sub = bfs(projectNodeId, 2);

        var sb = new StringBuilder();
        sb.append("## Nodos\n");
        for (GraphNode node : sub.nodes()) {
            sb.append("- **").append(node.label()).append("** (")
              .append(node.type()).append("): ").append(node.id()).append("\n");
        }
        sb.append("## Aristas\n");
        for (GraphEdge edge : sub.edges()) {
            sb.append("- ").append(edge.from()).append(" → ").append(edge.to())
              .append(" [").append(edge.type()).append(", conf=").append(edge.confidence()).append("]\n");
        }

        String result = sb.toString();
        if (result.length() > maxTokens) {
            result = result.substring(0, maxTokens);
        }
        return result;
    }
}
