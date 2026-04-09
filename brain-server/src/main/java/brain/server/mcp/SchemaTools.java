package brain.server.mcp;

import brain.server.config.BrainServerConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SchemaTools {

    @Value("${brain.schema-file:~/brain/SCHEMA.md}")
    private String schemaFileRaw;

    @Value("${brain.wiki-root:~/brain/wiki}")
    private String wikiRoot;

    @Value("${brain.raw-sources:~/brain/raw}")
    private String rawSources;

    @Value("${brain.graph-db:~/brain/brain_graph.db}")
    private String graphDb;

    @Value("${brain.ai.extraction-model:claude-haiku-4-5-20251001}")
    private String extractionModel;

    @Value("${brain.ai.wiki-write-model:claude-sonnet-4-6}")
    private String wikiWriteModel;

    @Value("${brain.ai.query-model:claude-sonnet-4-6}")
    private String queryModel;

    @Value("${brain.ai.lint-model:claude-haiku-4-5-20251001}")
    private String lintModel;

    @Value("${brain.graph.max-bfs-hops:3}")
    private int maxBfsHops;

    @Value("${brain.graph.max-context-tokens:2000}")
    private int maxContextTokens;

    @Value("${brain.graph.god-nodes-top-n:10}")
    private int godNodesTopN;

    @Tool(description = "Read SCHEMA.md which defines the knowledge taxonomy: node types, edge types, and frontmatter conventions.")
    public String schema_read() {
        Path path = BrainServerConfig.expand(schemaFileRaw);
        if (!Files.exists(path)) {
            return "SCHEMA.md not found at: " + path;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Tool(description = "Read the brain configuration: paths and model settings. Does not include secrets or API keys.")
    public Map<String, Object> config_read() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("wikiRoot", wikiRoot);
        cfg.put("rawSources", rawSources);
        cfg.put("graphDb", graphDb);
        cfg.put("schemaFile", schemaFileRaw);
        cfg.put("models", Map.of(
            "extractionModel", extractionModel,
            "wikiWriteModel", wikiWriteModel,
            "queryModel", queryModel,
            "lintModel", lintModel
        ));
        cfg.put("graph", Map.of(
            "maxBfsHops", maxBfsHops,
            "maxContextTokens", maxContextTokens,
            "godNodesTopN", godNodesTopN
        ));
        return cfg;
    }
}
