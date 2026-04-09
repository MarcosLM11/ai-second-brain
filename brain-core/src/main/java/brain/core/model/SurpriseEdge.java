package brain.core.model;

public record SurpriseEdge(
    String from,
    String to,
    double weight
) {}
