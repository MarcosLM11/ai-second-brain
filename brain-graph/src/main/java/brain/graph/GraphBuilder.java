package brain.graph;

import brain.core.model.EdgeOrigin;
import brain.core.model.EdgeType;
import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.port.WikiStore;
import brain.wiki.WikilinkExtractor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Builds the wiki knowledge graph from wikilinks.
 *
 * <p>Incremental builds skip pages whose SHA-256 content hash has not changed
 * since the last run (tracked in the {@code page_cache} SQLite table).
 * Pages are processed in parallel using virtual threads.
 */
public class GraphBuilder {

    private final WikiStore wiki;
    private final GraphStoreSqlite graph;

    public GraphBuilder(WikiStore wiki, GraphStoreSqlite graph) {
        this.wiki = wiki;
        this.graph = graph;
    }

    public BuildStats build(boolean force) {
        var pages = wiki.listAll();

        Map<String, String> sha256ByPage = pages.stream()
            .collect(Collectors.toMap(p -> p.id(), p -> sha256(p.content())));

        var toProcess = force
            ? pages
            : pages.stream()
                .filter(p -> !graph.isPageCached(p.id(), sha256ByPage.get(p.id())))
                .toList();

        // Extract edges in parallel with virtual threads
        List<PageEdges> results = extractInParallel(toProcess);

        // Write to SQLite sequentially
        var now = Instant.now();
        var allNewEdges = new ArrayList<GraphEdge>();

        for (int i = 0; i < toProcess.size(); i++) {
            var page = toProcess.get(i);
            var pe = results.get(i);
            graph.deleteEdgesFrom(pe.pageId());
            graph.saveNode(new GraphNode(
                page.id(), page.title(), page.type(), -1, page.created(), page.updated()
            ));
            allNewEdges.addAll(pe.edges());
            graph.updatePageCache(page.id(), sha256ByPage.get(page.id()));
        }

        // Ensure target nodes exist (INSERT OR IGNORE — don't overwrite existing metadata)
        for (GraphEdge e : allNewEdges) {
            graph.insertNodeIfAbsent(new GraphNode(e.to(), e.to(), brain.core.model.NodeType.CONCEPT, -1, now, now));
        }

        graph.insertEdges(allNewEdges);

        var loaded = graph.load();
        return new BuildStats(loaded.vertexSet().size(), loaded.edgeSet().size(), results.size());
    }

    private List<PageEdges> extractInParallel(List<brain.core.model.WikiPage> pages) {
        if (pages.isEmpty()) return List.of();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = pages.stream()
                .map(p -> executor.submit(() -> extractEdges(p)))
                .toList();
            return futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e.getCause());
                    }
                })
                .toList();
        }
    }

    private PageEdges extractEdges(brain.core.model.WikiPage page) {
        var links = WikilinkExtractor.extract(page.content());
        var edges = links.stream()
            .distinct()
            .map(link -> new GraphEdge(
                page.id(), link, EdgeType.LINKS_TO, EdgeOrigin.EXTRACTED, 1.0, Instant.now()
            ))
            .toList();
        return new PageEdges(page.id(), edges);
    }

    static String sha256(String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
