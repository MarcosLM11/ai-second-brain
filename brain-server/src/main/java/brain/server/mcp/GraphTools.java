package brain.server.mcp;

import brain.core.model.WikiPage;
import brain.core.port.WikiStore;
import brain.graph.GraphBuilder;
import brain.wiki.WikilinkExtractor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * MCP tools for graph-based wiki traversal.
 *
 * <p>{@code graph_query} performs relevance scoring + BFS expansion over wikilinks.
 * Full JGraphT/SQLite-backed graph traversal will replace this in a later phase.
 */
@Component
public class GraphTools {

    private final WikiStore wikiStore;
    private final GraphBuilder graphBuilder;

    public GraphTools(WikiStore wikiStore, GraphBuilder graphBuilder) {
        this.wikiStore = wikiStore;
        this.graphBuilder = graphBuilder;
    }

    @Tool(description = """
        Build the knowledge graph from wiki wikilinks.
        Reads all wiki pages, extracts [[wikilinks]], and persists nodes and LINKS_TO edges in SQLite.
        Incremental by default: only reprocesses pages whose content has changed (SHA-256).
        Use force=true to reprocess all pages regardless of cache.
        Returns stats: nodes, edges, and pages processed.
        """)
    public String graph_build(
        @ToolParam(description = "If true, reprocess all pages ignoring the incremental cache") boolean force
    ) {
        var stats = graphBuilder.build(force);
        return "Graph built: %d nodes, %d edges, %d pages processed"
            .formatted(stats.nodes(), stats.edges(), stats.pagesProcessed());
    }

    @Tool(description = """
        Find wiki pages relevant to a topic using relevance scoring + BFS over wikilinks.
        Seeds are chosen by keyword match (title > tags > content).
        BFS expands one hop via [[wikilinks]] from each seed.
        Returns up to maxPages page IDs, capped at 8.
        """)
    public List<String> graph_query(
        @ToolParam(description = "Topic or keywords to search for") String topic,
        @ToolParam(description = "Maximum number of page IDs to return (1–8)") int maxPages
    ) {
        int limit = Math.min(Math.max(maxPages, 1), 8);
        List<WikiPage> all = wikiStore.listAll();
        if (all.isEmpty()) return List.of();

        String[] terms = topic.toLowerCase().split("\\s+");

        // Score every page by relevance to the topic
        Map<String, Integer> scores = new HashMap<>();
        for (WikiPage page : all) {
            int s = relevanceScore(page, terms);
            if (s > 0) scores.put(page.id(), s);
        }
        if (scores.isEmpty()) return List.of();

        // Start BFS from the top-3 seeds
        Set<String> result = new LinkedHashSet<>();
        scores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .map(Map.Entry::getKey)
            .forEach(result::add);

        // BFS: expand one hop via wikilinks from each seed
        for (String seedId : new ArrayList<>(result)) {
            if (result.size() >= limit) break;
            wikiStore.read(seedId).ifPresent(page ->
                WikilinkExtractor.extract(page.content()).stream()
                    .map(link -> resolveLink(link, all))
                    .filter(Objects::nonNull)
                    .filter(id -> !result.contains(id))
                    .forEach(id -> {
                        if (result.size() < limit) result.add(id);
                    })
            );
        }

        List<String> resultList = new ArrayList<>(result);
        return resultList.subList(0, Math.min(resultList.size(), limit));
    }

    // --- helpers ---

    private int relevanceScore(WikiPage page, String[] terms) {
        int score = 0;
        String title = page.title().toLowerCase();
        String content = page.content().toLowerCase();
        for (String term : terms) {
            if (title.contains(term)) score += 3;
            for (String tag : page.tags()) {
                if (tag.toLowerCase().contains(term)) score += 2;
            }
            if (content.contains(term)) score += 1;
        }
        return score;
    }

    /**
     * Resolves a raw wikilink reference to a wiki page ID by trying:
     * 1. Exact ID match
     * 2. Slug match (lowercase, spaces → hyphens) against page ID suffix
     * 3. Case-insensitive title match
     */
    private String resolveLink(String wikilink, List<WikiPage> allPages) {
        String slug = wikilink.toLowerCase().replace(' ', '-');
        for (WikiPage p : allPages) {
            if (p.id().equalsIgnoreCase(wikilink)) return p.id();
            if (p.id().equals(slug) || p.id().endsWith("/" + slug)) return p.id();
            if (p.title().equalsIgnoreCase(wikilink)) return p.id();
        }
        return null;
    }
}
