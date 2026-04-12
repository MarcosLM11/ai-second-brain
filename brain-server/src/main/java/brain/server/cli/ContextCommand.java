package brain.server.cli;

import brain.core.config.BrainConfig;
import brain.core.config.BrainConfigLoader;
import brain.core.config.ModelConfig;
import brain.graph.GraphStoreSqlite;
import brain.graph.GraphTraversal;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import java.nio.file.Path;

@Command(
    name = "context",
    description = "Inject relevant knowledge graph context for the current project session"
)
public class ContextCommand implements Runnable {

    @ParentCommand
    BrainCli parent;

    @Option(
        names = {"--project", "-p"},
        description = "Project directory to use as the starting node for BFS context",
        paramLabel = "DIR",
        defaultValue = "."
    )
    Path projectDir;

    @Option(
        names = {"--max-tokens"},
        description = "Maximum character length of injected context (default: 2000)",
        defaultValue = "2000"
    )
    int maxTokens;

    @Override
    public void run() {
        try {
            BrainConfig config = loadConfig();

            var store = new GraphStoreSqlite(config.graphDbPath());
            var traversal = new GraphTraversal(store);

            String nodeId = projectDir.toAbsolutePath().getFileName().toString();

            String context = traversal.buildSessionContext(nodeId, maxTokens);
            if (!context.isBlank()) {
                System.out.print(context);
            }
        } catch (Exception e) {
            // Silently exit — do not block the session on errors
            System.err.println("[brain context] " + e.getMessage());
        }
    }

    private BrainConfig loadConfig() throws Exception {
        // Priority: parent --config flag → brain.toml in project dir → ~/brain/brain.toml
        if (parent != null && parent.configFile != null) {
            return BrainConfigLoader.load(parent.configFile);
        }
        Path local = projectDir.resolve("brain.toml");
        if (local.toFile().exists()) {
            return BrainConfigLoader.load(local);
        }
        Path home = Path.of(System.getProperty("user.home"), "brain", "brain.toml");
        if (home.toFile().exists()) {
            return BrainConfigLoader.load(home);
        }
        Path projectRoot = Path.of(System.getProperty("user.home"), "Projects", "Software-Engineer", "ai-second-brain", "brain.toml");
        if (projectRoot.toFile().exists()) {
            return BrainConfigLoader.load(projectRoot);
        }
        // Return a minimal default config
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
