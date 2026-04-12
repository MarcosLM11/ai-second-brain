package brain.server.cli;

import brain.ai.ExtractionResult;
import brain.ai.ExtractorService;
import brain.core.config.BrainConfig;
import brain.core.config.BrainConfigLoader;
import brain.core.config.ModelConfig;
import brain.graph.CacheStoreSqlite;
import brain.graph.GraphBuilder;
import brain.graph.GraphStoreSqlite;
import brain.wiki.WikiStoreFs;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * CLI command for ingesting a source file into the brain wiki.
 *
 * <p>Usage: {@code brain ingest --source-type=conversation --file=<path> [--async]}
 *
 * <p>With {@code --async}, the ingestion runs in a Virtual Thread and
 * {@code run()} returns immediately. The JVM stays alive until the thread completes
 * (requires {@code BrainApplication.main} to not call {@code System.exit(0)}).
 */
@Command(
    name = "ingest",
    description = "Ingest a source file into the brain wiki"
)
public class IngestCommand implements Runnable {

    @ParentCommand
    BrainCli parent;

    @Option(
        names = {"--source-type"},
        description = "Source type: conversation, file (default: file)",
        defaultValue = "file"
    )
    String sourceType;

    @Option(
        names = {"--file", "-f"},
        description = "Path to file to ingest",
        required = true
    )
    Path file;

    @Option(
        names = {"--async"},
        description = "Run ingestion in a background Virtual Thread and return immediately"
    )
    boolean async;

    @Override
    public void run() {
        if (async) {
            Thread vt = Thread.ofVirtual().name("brain-ingest").start(this::runIngestion);
            // Add shutdown hook so the JVM waits for the Virtual Thread before exiting
            Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(() -> {
                try {
                    vt.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        } else {
            runIngestion();
        }
    }

    private void runIngestion() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("[brain ingest] ERROR: ANTHROPIC_API_KEY is not set");
            return;
        }

        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            System.err.println("[brain ingest] ERROR reading file: " + e.getMessage());
            return;
        }

        try {
            BrainConfig config = loadConfig();

            var api = AnthropicApi.builder().apiKey(apiKey).build();
            var extractorModel = AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(AnthropicChatOptions.builder()
                    .model(config.models().extractionModel())
                    .build())
                .build();
            var extractor = new ExtractorService(ChatClient.builder(extractorModel).build());

            var wikiStore = new WikiStoreFs(config.wikiRoot());
            String wikiIndex = wikiStore.readRaw("index.md").orElse("");

            ExtractionResult result = extractor.extract(content, wikiIndex);

            if (result.newPages().isEmpty() && result.pageUpdates().isEmpty()) {
                System.out.println("[brain ingest] No content extracted (transcript may be too short)");
                return;
            }

            var writerModel = AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(AnthropicChatOptions.builder()
                    .model(config.models().wikiWriteModel())
                    .build())
                .build();
            var writer = new brain.ai.WikiWriterService(ChatClient.builder(writerModel).build());

            int created = 0;
            for (var page : result.newPages()) {
                String pageContent = writer.writePage(
                    "Create a wiki page titled \"" + page.title() + "\". Summary: " + page.summary(),
                    null, null, null
                );
                wikiStore.write(page.pageId(), pageContent);
                created++;
            }

            int updated = 0;
            for (var update : result.pageUpdates()) {
                String existing = wikiStore.read(update.pageId())
                    .map(p -> p.content())
                    .orElse(null);
                String pageContent = writer.writePage(
                    "Update this page with: " + update.summary(),
                    null, existing, null
                );
                wikiStore.write(update.pageId(), pageContent);
                updated++;
            }

            // Update cache
            var cacheStore = new CacheStoreSqlite(config.graphDbPath());
            String hash = Integer.toHexString(content.hashCode());
            cacheStore.set(hash, "{\"source\":\"" + file + "\",\"date\":\"" + Instant.now() + "\"}");

            // Rebuild graph incrementally
            var graphStore = new GraphStoreSqlite(config.graphDbPath());
            var graphBuilder = new GraphBuilder(wikiStore, graphStore);
            graphBuilder.build(false);

            System.out.printf("[brain ingest] Done: %d created, %d updated%n", created, updated);

        } catch (Exception e) {
            System.err.println("[brain ingest] ERROR: " + e.getMessage());
        }
    }

    private BrainConfig loadConfig() throws Exception {
        if (parent != null && parent.configFile != null) {
            return BrainConfigLoader.load(parent.configFile);
        }
        Path home = Path.of(System.getProperty("user.home"), "brain", "brain.toml");
        if (home.toFile().exists()) {
            return BrainConfigLoader.load(home);
        }
        return new BrainConfig(
            Path.of(System.getProperty("user.home"), "brain", "wiki"),
            Path.of(System.getProperty("user.home"), "brain", "raw"),
            Path.of(System.getProperty("user.home"), "brain", "brain_graph.db"),
            Path.of(System.getProperty("user.home"), "brain", "SCHEMA.md"),
            "UTC", 2000, 3, 10, 5, 0.7, true, 500,
            new ModelConfig(
                "claude-haiku-4-5-20251001",
                "claude-sonnet-4-6",
                "claude-sonnet-4-6",
                "claude-haiku-4-5-20251001"
            )
        );
    }
}
