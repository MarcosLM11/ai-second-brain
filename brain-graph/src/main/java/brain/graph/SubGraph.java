package brain.graph;

import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import java.util.List;

public record SubGraph(List<GraphNode> nodes, List<GraphEdge> edges) {}
