package brain.server.cli;

import brain.core.config.BrainConfig;
import brain.core.config.BrainConfigLoader;
import brain.core.config.ModelConfig;
import brain.graph.UsageTracker;
import brain.graph.UsageTracker.UsageStat;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * CLI command for displaying accumulated token cost statistics.
 *
 * <p>Usage:
 * <pre>
 *   brain stats
 *   brain stats --reset
 * </pre>
 */
@Command(
    name = "stats",
    description = "Show accumulated token cost statistics",
    mixinStandardHelpOptions = true
)
public class StatsCommand implements Runnable {

    // Default prices per million tokens (MTok) in USD — Anthropic pricing as of 2026-04
    private static final Map<String, double[]> DEFAULT_PRICES = Map.of(
        "claude-haiku-4-5-20251001",  new double[]{0.80,  4.00},
        "claude-sonnet-4-6",          new double[]{3.00, 15.00},
        "claude-opus-4-6",            new double[]{15.00, 75.00}
    );

    @ParentCommand
    BrainCli parent;

    @Option(
        names = {"--reset"},
        description = "Clear all accumulated usage records"
    )
    boolean reset;

    @Override
    public void run() {
        try {
            BrainConfig config = loadConfig();
            UsageTracker tracker = new UsageTracker(config.graphDbPath());

            if (reset) {
                tracker.reset();
                System.out.println("[brain stats] Usage log cleared.");
                return;
            }

            List<UsageStat> stats = tracker.getStats();
            if (stats.isEmpty()) {
                System.out.println("[brain stats] No usage recorded yet.");
                return;
            }

            printTable(stats);

        } catch (Exception e) {
            System.err.println("[brain stats] ERROR: " + e.getMessage());
        }
    }

    private void printTable(List<UsageStat> stats) {
        String fmt = "%-20s %-35s %10s %11s %6s %10s%n";
        System.out.printf(fmt, "OPERATION", "MODEL", "INPUT(tok)", "OUTPUT(tok)", "CALLS", "COST(USD)");
        System.out.println("-".repeat(100));

        double grandTotal = 0.0;
        for (UsageStat stat : stats) {
            double cost = estimateCost(stat.model(), stat.inputTokens(), stat.outputTokens());
            grandTotal += cost;
            System.out.printf(fmt,
                truncate(stat.operation(), 20),
                truncate(stat.model(), 35),
                stat.inputTokens(),
                stat.outputTokens(),
                stat.calls(),
                "$%.5f".formatted(cost)
            );
        }

        System.out.println("-".repeat(100));
        System.out.printf("%-20s %-35s %10s %11s %6s %10s%n",
            "TOTAL", "", "", "", "", "$%.5f".formatted(grandTotal));
        System.out.printf("%nEstimated monthly cost (×30 days): $%.4f%n", grandTotal * 30);
    }

    private double estimateCost(String model, long inputTokens, long outputTokens) {
        double[] prices = DEFAULT_PRICES.getOrDefault(model, new double[]{3.00, 15.00});
        return (inputTokens / 1_000_000.0) * prices[0]
             + (outputTokens / 1_000_000.0) * prices[1];
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
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
