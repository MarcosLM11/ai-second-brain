package brain.search;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BM25 full-text search over the FTS5 index maintained by {@link SearchIndexer}.
 *
 * <p>Uses SQLite's native {@code bm25()} ranking function and {@code snippet()}
 * for contextual excerpts. Both functions are built into FTS5 and require no
 * external libraries.
 */
public class SearchEngine {

    private static final int SNIPPET_TOKENS_BEFORE = 10;
    private static final int SNIPPET_TOKENS_AFTER  = 10;
    private static final int SNIPPET_MAX_PHRASES   = 1;

    private final String url;

    public SearchEngine(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    /**
     * Searches the FTS5 index and returns ranked results.
     *
     * @param query  full-text query (supports FTS5 phrase/prefix syntax)
     * @param limit  maximum number of results to return (must be ≥ 1)
     * @return list of {@link SearchResult} ordered by BM25 relevance (best first),
     *         empty list when {@code query} is blank
     */
    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int clampedLimit = Math.max(1, limit);

        String sql = """
            SELECT
                page_id,
                bm25(wiki_fts) AS score,
                snippet(wiki_fts, 2, '<b>', '</b>', '…', ?)
            FROM wiki_fts
            WHERE wiki_fts MATCH ?
            ORDER BY score
            LIMIT ?
            """;

        List<SearchResult> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, SNIPPET_TOKENS_BEFORE + SNIPPET_TOKENS_AFTER);
            ps.setString(2, query);
            ps.setInt(3, clampedLimit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new SearchResult(
                        rs.getString(1),
                        rs.getDouble(2),
                        rs.getString(3)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search failed for query: " + query, e);
        }
        return results;
    }
}
