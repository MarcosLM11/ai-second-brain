package brain.core.model;

import java.util.Set;

public record Community(
    int         id,
    Set<String> nodeIds
) {}
