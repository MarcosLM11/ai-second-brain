package brain.graph;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks token usage and estimated cost per operation in SQLite (RNF-COST-01, RNF-COST-04).
 *
 * <p>Call {@link #record} after every LLM operation. Use {@link #getStats} to retrieve
 * the accumulated cost breakdown, and {@link #reset} to clear all records.
 */
public class UsageTracker {

    private final String url;

    public UsageTracker(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        try (Connection conn = connect(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS usage_log (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    operation    TEXT NOT NULL,
                    model        TEXT NOT NULL,
                    input_tokens INTEGER NOT NULL,
                    output_tokens INTEGER NOT NULL,
                    recorded_at  INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize usage_log schema", e);
        }
    }

    /**
     * Records a single LLM call's token usage.
     *
     * @param operation    logical operation name: ingest, query, lint, capture, etc.
     * @param model        model ID used for the call
     * @param inputTokens  number of prompt tokens consumed
     * @param outputTokens number of completion tokens produced
     */
    public void record(String operation, String model, int inputTokens, int outputTokens) {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO usage_log (operation, model, input_tokens, output_tokens, recorded_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, operation);
            ps.setString(2, model);
            ps.setInt(3, inputTokens);
            ps.setInt(4, outputTokens);
            ps.setLong(5, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Non-critical: don't crash the main operation due to tracking failure
            System.err.println("[UsageTracker] Warning: failed to record usage – " + e.getMessage());
        }
    }

    /**
     * Returns aggregated usage grouped by operation and model.
     */
    public List<UsageStat> getStats() {
        List<UsageStat> stats = new ArrayList<>();
        String sql = """
            SELECT operation, model,
                   SUM(input_tokens)  AS total_input,
                   SUM(output_tokens) AS total_output,
                   COUNT(*)           AS calls
            FROM usage_log
            GROUP BY operation, model
            ORDER BY operation, model
            """;
        try (Connection conn = connect();
             var stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                stats.add(new UsageStat(
                    rs.getString("operation"),
                    rs.getString("model"),
                    rs.getLong("total_input"),
                    rs.getLong("total_output"),
                    rs.getLong("calls")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query usage stats", e);
        }
        return stats;
    }

    /**
     * Deletes all usage records.
     */
    public void reset() {
        try (Connection conn = connect(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM usage_log");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset usage log", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    /**
     * Aggregated usage row returned by {@link #getStats()}.
     */
    public record UsageStat(
        String operation,
        String model,
        long   inputTokens,
        long   outputTokens,
        long   calls
    ) {}
}
