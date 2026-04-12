package brain.graph;

import brain.core.model.GraphEdge;
import java.util.List;

record PageEdges(String pageId, List<GraphEdge> edges) {}
