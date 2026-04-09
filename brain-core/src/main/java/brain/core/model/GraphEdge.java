package brain.core.model;

import java.time.Instant;

public record GraphEdge(
    String     from,
    String     to,
    EdgeType   type,
    EdgeOrigin origin,
    double     confidence,
    Instant    timestamp
) {}
