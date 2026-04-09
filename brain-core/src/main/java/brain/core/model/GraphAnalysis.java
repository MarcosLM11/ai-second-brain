package brain.core.model;

import java.util.List;
import java.util.Map;

public record GraphAnalysis(
    List<GraphNode>     godNodes,
    List<Community>     communities,
    List<SurpriseEdge>  surprises,
    Map<String, Double> expertiseMap
) {}
