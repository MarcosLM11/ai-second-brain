package brain.search;

/**
 * A single result from a BM25 full-text search.
 *
 * @param pageId  wiki page identifier (e.g. "concepts/jwt")
 * @param score   BM25 relevance score (lower is better in SQLite's bm25())
 * @param snippet excerpt showing the query terms in context
 */
public record SearchResult(String pageId, double score, String snippet) {}
