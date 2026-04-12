package brain.server.mcp;

import brain.core.port.WikiStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class WikiTools {

    private final WikiStore wikiStore;

    public WikiTools(WikiStore wikiStore) {
        this.wikiStore = wikiStore;
    }

    @Tool(description = "Read a wiki page by its ID (relative path without .md, e.g. 'concepts/jwt'). Returns the raw markdown content.")
    public String wiki_read(
        @ToolParam(description = "Page ID: relative path without .md extension") String pageId
    ) {
        return wikiStore.read(pageId)
            .map(p -> p.content())
            .orElse("Page not found: " + pageId);
    }

    @Tool(description = "Write or overwrite a wiki page. Creates parent directories automatically.")
    public String wiki_write(
        @ToolParam(description = "Page ID: relative path without .md extension") String pageId,
        @ToolParam(description = "Full markdown content including YAML frontmatter") String content
    ) {
        wikiStore.write(pageId, content);
        return "Written: " + pageId;
    }

    @Tool(description = "List wiki pages, optionally filtered by type (concept, entity, decision, question, source) and/or glob pattern.")
    public List<Map<String, String>> wiki_list(
        @ToolParam(description = "Node type filter: concept, entity, decision, question, source. Empty string for all.") String type,
        @ToolParam(description = "Glob pattern, e.g. 'concepts/*'. Empty string for all.") String pattern
    ) {
        return wikiStore.list(type, pattern).stream()
            .map(p -> Map.of("id", p.id(), "title", p.title(), "type", p.type().name()))
            .toList();
    }

    @Tool(description = "Read the wiki index (index.md) which catalogues all pages with their title, type, and summary.")
    public String wiki_index_read() {
        return wikiStore.readRaw("index.md").orElse("# Index\n\n(empty — run wiki_list to explore pages)");
    }
}
