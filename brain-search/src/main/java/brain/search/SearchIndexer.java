package brain.search;

import brain.core.model.WikiPage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * Manages a SQLite FTS5 full-text search index for wiki pages.
 *
 * <p>The {@code wiki_fts} virtual table uses the Porter stemmer so that
 * morphological variants (e.g. "running" / "runs") match the same stem.
 */
public class SearchIndexer {

    private final String url;

    public SearchIndexer(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        try (var conn = connect(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE VIRTUAL TABLE IF NOT EXISTS wiki_fts USING fts5(
                    page_id UNINDEXED,
                    title,
                    content,
                    tags,
                    tokenize = 'porter unicode61'
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize FTS5 schema", e);
        }
    }

    /**
     * Inserts or replaces the FTS5 entry for a single page.
     */
    public void indexPage(WikiPage page) {
        try (var conn = connect()) {
            conn.setAutoCommit(false);
            try (var del = conn.prepareStatement("DELETE FROM wiki_fts WHERE page_id = ?");
                 var ins = conn.prepareStatement(
                     "INSERT INTO wiki_fts(page_id, title, content, tags) VALUES (?, ?, ?, ?)")) {
                del.setString(1, page.id());
                del.executeUpdate();
                ins.setString(1, page.id());
                ins.setString(2, page.title());
                ins.setString(3, page.content());
                ins.setString(4, String.join(" ", page.tags()));
                ins.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to index page: " + page.id(), e);
        }
    }

    /**
     * Clears the entire FTS5 index and rebuilds it from the given list.
     */
    public void indexAll(List<WikiPage> pages) {
        try (var conn = connect()) {
            conn.setAutoCommit(false);
            try (var del = conn.prepareStatement("DELETE FROM wiki_fts");
                 var ins = conn.prepareStatement(
                     "INSERT INTO wiki_fts(page_id, title, content, tags) VALUES (?, ?, ?, ?)")) {
                del.executeUpdate();
                for (WikiPage page : pages) {
                    ins.setString(1, page.id());
                    ins.setString(2, page.title());
                    ins.setString(3, page.content());
                    ins.setString(4, String.join(" ", page.tags()));
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to index all pages", e);
        }
    }

    /**
     * Returns the number of rows in the FTS index. Useful for tests and diagnostics.
     */
    public long countRows() {
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM wiki_fts")) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count FTS rows", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
