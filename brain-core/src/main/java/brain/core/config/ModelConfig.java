package brain.core.config;

public record ModelConfig(
    String extractionModel,
    String wikiWriteModel,
    String queryModel,
    String lintModel
) {}
