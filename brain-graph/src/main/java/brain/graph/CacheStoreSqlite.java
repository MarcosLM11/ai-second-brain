package brain.graph;

import brain.core.port.CacheStore;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * SQLite-backed {@link CacheStore}.
 *
 * <p>Uses the {@code page_cache} table: {@code (page_id TEXT PK, sha256 TEXT, processed_at INTEGER)}.
 * The {@code page_id} is the sha256 hash itself, making sha256 the natural cache key.
 */
public class CacheStoreSqlite implements CacheStore {

    private final String url;

    public CacheStoreSqlite(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        try (var conn = connect(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS page_cache (
                    page_id      TEXT PRIMARY KEY,
                    sha256       TEXT NOT NULL,
                    processed_at INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize page_cache schema", e);
        }
    }

    @Override
    public boolean isHit(String sha256) {
        try (var conn = connect();
             var ps = conn.prepareStatement(
                 "SELECT 1 FROM page_cache WHERE page_id = ?")) {
            ps.setString(1, sha256);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check cache: " + sha256, e);
        }
    }

    @Override
    public Optional<String> getLastProcessed(String sha256) {
        try (var conn = connect();
             var ps = conn.prepareStatement(
                 "SELECT processed_at FROM page_cache WHERE page_id = ?")) {
            ps.setString(1, sha256);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    Instant instant = Instant.ofEpochMilli(rs.getLong("processed_at"));
                    return Optional.of(instant.toString());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get last processed: " + sha256, e);
        }
        return Optional.empty();
    }

    @Override
    public void set(String sha256, String metadata) {
        try (var conn = connect();
             var ps = conn.prepareStatement("""
                 INSERT OR REPLACE INTO page_cache (page_id, sha256, processed_at)
                 VALUES (?, ?, ?)
                 """)) {
            ps.setString(1, sha256);
            ps.setString(2, sha256);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set cache: " + sha256, e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
