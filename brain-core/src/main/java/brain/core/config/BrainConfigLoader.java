package brain.core.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlFactory;
import java.io.IOException;
import java.nio.file.Path;

public final class BrainConfigLoader {

    private static final ObjectMapper TOML = new ObjectMapper(new TomlFactory());

    private BrainConfigLoader() {}

    public static BrainConfig load(Path configPath) throws IOException {
        JsonNode root = TOML.readTree(configPath.toFile());

        JsonNode paths   = root.path("paths");
        JsonNode models  = root.path("models");
        JsonNode graph   = root.path("graph");
        JsonNode capture = root.path("capture");

        return new BrainConfig(
            expandPath(paths.path("wiki_root").asText("~/brain/wiki")),
            expandPath(paths.path("raw_sources").asText("~/brain/raw")),
            expandPath(paths.path("graph_db").asText("~/brain/brain_graph.db")),
            expandPath(paths.path("schema_file").asText("~/brain/SCHEMA.md")),
            root.path("timezone").asText("UTC"),
            graph.path("max_context_tokens").asInt(2000),
            graph.path("max_bfs_hops").asInt(3),
            graph.path("god_nodes_top_n").asInt(10),
            graph.path("community_count").asInt(5),
            graph.path("surprise_min_conf").asDouble(0.7),
            capture.path("enabled").asBoolean(true),
            capture.path("min_session_tokens").asInt(500),
            new ModelConfig(
                models.path("extraction").asText("claude-haiku-4-5-20251001"),
                models.path("wiki_write").asText("claude-sonnet-4-6"),
                models.path("query").asText("claude-sonnet-4-6"),
                models.path("lint").asText("claude-haiku-4-5-20251001")
            )
        );
    }

    private static Path expandPath(String raw) {
        if (raw.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), raw.substring(2));
        }
        return Path.of(raw);
    }
}
