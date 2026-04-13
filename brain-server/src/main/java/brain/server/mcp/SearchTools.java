package brain.server.mcp;

import brain.core.model.WikiPage;
import brain.core.port.WikiStore;
import brain.search.SearchEngine;
import brain.search.SearchIndexer;
import brain.search.SearchResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SearchTools {

    private final SearchIndexer searchIndexer;
    private final SearchEngine searchEngine;
    private final WikiStore wikiStore;

    public SearchTools(SearchIndexer searchIndexer, SearchEngine searchEngine, WikiStore wikiStore) {
        this.searchIndexer = searchIndexer;
        this.searchEngine = searchEngine;
        this.wikiStore = wikiStore;
    }

    @Tool(description = """
        Update the full-text search (FTS5) index.
        If pageId is provided, updates only that page's index entry (incremental update).
        If pageId is empty, rebuilds the entire index from all wiki pages.
        Returns the number of pages indexed or a confirmation for single-page updates.
        """)
    public String search_index_update(
        @ToolParam(description = "Page ID to update (e.g. 'concepts/jwt'). Leave empty to rebuild the full index.") String pageId
    ) {
        if (pageId == null || pageId.isBlank()) {
            List<WikiPage> pages = wikiStore.listAll();
            searchIndexer.indexAll(pages);
            return "Search index rebuilt: %d pages indexed.".formatted(pages.size());
        }
        return wikiStore.read(pageId)
            .map(page -> {
                searchIndexer.indexPage(page);
                return "Search index updated for: " + pageId;
            })
            .orElse("Page not found: " + pageId);
    }

    @Tool(description = """
        Search the wiki using BM25 full-text search (FTS5).
        Returns ranked results with page ID, relevance score, and a snippet showing
        the query terms in context.
        Returns an empty list when query is blank.
        Call search_index_update first if the index may be stale.
        """)
    public String search(
        @ToolParam(description = "Full-text query. Supports FTS5 phrase syntax, e.g. \"transformer attention\"") String query,
        @ToolParam(description = "Maximum number of results to return (default 10)") Integer limit
    ) {
        int effectiveLimit = (limit == null || limit <= 0) ? 10 : limit;
        List<SearchResult> results = searchEngine.search(query, effectiveLimit);
        if (results.isEmpty()) {
            return "No results found for: " + query;
        }
        return results.stream()
            .map(r -> "pageId: %s | score: %.4f | snippet: %s".formatted(r.pageId(), r.score(), r.snippet()))
            .collect(Collectors.joining("\n"));
    }
}
