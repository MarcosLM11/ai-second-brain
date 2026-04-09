package brain.core.config;

import java.nio.file.Path;

public record BrainConfig(
    Path        wikiRoot,
    Path        rawSourcesRoot,
    Path        graphDbPath,
    Path        schemaFile,
    String      timezone,
    int         maxContextTokens,
    int         maxBfsHops,
    int         godNodesTopN,
    int         communityCount,
    double      surpriseMinConf,
    boolean     captureEnabled,
    int         minSessionTokens,
    ModelConfig models
) {}
