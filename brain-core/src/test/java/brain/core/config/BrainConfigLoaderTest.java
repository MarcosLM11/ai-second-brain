package brain.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BrainConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsFullConfig() throws IOException {
        Path configPath = tempDir.resolve("brain.toml");
        Files.writeString(configPath, """
                [paths]
                wiki_root   = "~/brain/wiki"
                raw_sources = "~/brain/raw"
                graph_db    = "~/brain/brain_graph.db"
                schema_file = "~/brain/SCHEMA.md"

                [models]
                extraction = "claude-haiku-4-5-20251001"
                wiki_write = "claude-sonnet-4-6"
                query      = "claude-sonnet-4-6"
                lint       = "claude-haiku-4-5-20251001"

                [graph]
                max_bfs_hops       = 3
                max_context_tokens = 2000
                god_nodes_top_n    = 10
                community_count    = 5
                surprise_min_conf  = 0.7

                [capture]
                enabled            = true
                min_session_tokens = 500
                """);

        BrainConfig config = BrainConfigLoader.load(configPath);

        assertThat(config.wikiRoot())
            .isEqualTo(Path.of(System.getProperty("user.home"), "brain/wiki"));
        assertThat(config.models().extractionModel())
            .isEqualTo("claude-haiku-4-5-20251001");
        assertThat(config.models().wikiWriteModel())
            .isEqualTo("claude-sonnet-4-6");
        assertThat(config.maxBfsHops()).isEqualTo(3);
        assertThat(config.maxContextTokens()).isEqualTo(2000);
        assertThat(config.godNodesTopN()).isEqualTo(10);
        assertThat(config.surpriseMinConf()).isEqualTo(0.7);
        assertThat(config.captureEnabled()).isTrue();
        assertThat(config.minSessionTokens()).isEqualTo(500);
    }

    @Test
    void expandsTildeInPaths() throws IOException {
        Path configPath = tempDir.resolve("brain.toml");
        Files.writeString(configPath, """
                [paths]
                wiki_root   = "~/my-wiki"
                raw_sources = "~/raw"
                graph_db    = "~/graph.db"
                schema_file = "~/schema.md"
                """);

        BrainConfig config = BrainConfigLoader.load(configPath);

        String home = System.getProperty("user.home");
        assertThat(config.wikiRoot().toString()).startsWith(home);
        assertThat(config.rawSourcesRoot().toString()).startsWith(home);
    }

    @Test
    void usesDefaultsWhenSectionsAbsent() throws IOException {
        Path configPath = tempDir.resolve("brain.toml");
        Files.writeString(configPath, """
                [paths]
                wiki_root   = "/tmp/wiki"
                raw_sources = "/tmp/raw"
                graph_db    = "/tmp/graph.db"
                schema_file = "/tmp/schema.md"
                """);

        BrainConfig config = BrainConfigLoader.load(configPath);

        assertThat(config.maxBfsHops()).isEqualTo(3);
        assertThat(config.maxContextTokens()).isEqualTo(2000);
        assertThat(config.captureEnabled()).isTrue();
        assertThat(config.models().extractionModel()).isEqualTo("claude-haiku-4-5-20251001");
    }
}
