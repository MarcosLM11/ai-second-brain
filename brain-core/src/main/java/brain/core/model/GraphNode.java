package brain.core.model;

import java.time.Instant;

public record GraphNode(
    String   id,
    String   label,
    NodeType type,
    int      community,
    Instant  createdAt,
    Instant  updatedAt
) {}
