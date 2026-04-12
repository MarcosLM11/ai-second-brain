package brain.server.mcp;

import brain.core.model.WikiPage;
import brain.core.port.WikiStore;
import brain.search.SearchIndexer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchTools {

    private final SearchIndexer searchIndexer;
    private final WikiStore wikiStore;

    public SearchTools(SearchIndexer searchIndexer, WikiStore wikiStore) {
        this.searchIndexer = searchIndexer;
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
}
