package brain.core.port;

import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import java.util.List;
import java.util.Optional;

public interface GraphStore {

    void persist(Graph<String, DefaultWeightedEdge> graph, List<GraphEdge> edges);

    Graph<String, DefaultWeightedEdge> load();

    Optional<GraphNode> getNode(String id);

    void saveNode(GraphNode node);
}
