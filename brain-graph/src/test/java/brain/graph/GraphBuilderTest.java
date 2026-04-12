package brain.graph;

import brain.wiki.WikiStoreFs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GraphBuilderTest {

    @TempDir Path wikiDir;
    @TempDir Path dbDir;

    WikiStoreFs wikiStore;
    GraphStoreSqlite graphStore;
    GraphBuilder builder;

    @BeforeEach
    void setUp() {
        wikiStore = new WikiStoreFs(wikiDir);
        graphStore = new GraphStoreSqlite(dbDir.resolve("brain_graph.db"));
        builder = new GraphBuilder(wikiStore, graphStore);
    }

    // Helper to write a markdown wiki page
    private void writePage(String id, String title, String... links) {
        var sb = new StringBuilder();
        sb.append("---\ntitle: \"").append(title).append("\"\ntype: concept\ncreated: 2024-01-01\nupdated: 2024-01-01\n---\n");
        sb.append("# ").append(title).append("\n");
        for (String link : links) {
            sb.append("See [[").append(link).append("]].\n");
        }
        wikiStore.write(id, sb.toString());
    }

    @Test
    void buildWiki5PagesProducesCorrectGraph() {
        writePage("page-a", "Page A", "page-b", "page-c");
        writePage("page-b", "Page B", "page-d");
        writePage("page-c", "Page C", "page-e");
        writePage("page-d", "Page D");
        writePage("page-e", "Page E");

        var stats = builder.build(false);

        assertThat(stats.pagesProcessed()).isEqualTo(5);
        assertThat(stats.nodes()).isGreaterThanOrEqualTo(5);
        // page-a→b, page-a→c, page-b→d, page-c→e = 4 edges
        assertThat(stats.edges()).isEqualTo(4);

        var graph = graphStore.load();
        assertThat(graph.containsVertex("page-a")).isTrue();
        assertThat(graph.containsVertex("page-b")).isTrue();
        assertThat(graph.getEdge("page-a", "page-b")).isNotNull();
        assertThat(graph.getEdge("page-a", "page-c")).isNotNull();
        assertThat(graph.getEdge("page-b", "page-d")).isNotNull();
        assertThat(graph.getEdge("page-c", "page-e")).isNotNull();
    }

    @Test
    void secondRunWithoutChangesProcessesZeroPages() {
        writePage("page-a", "Page A", "page-b");
        writePage("page-b", "Page B");

        builder.build(false); // first run
        var stats = builder.build(false); // second run, nothing changed

        assertThat(stats.pagesProcessed()).isEqualTo(0);
        assertThat(stats.edges()).isEqualTo(1);
    }

    @Test
    void modifyOnePageOnlyThatPageReprocessed() {
        writePage("page-a", "Page A", "page-b");
        writePage("page-b", "Page B", "page-c");
        writePage("page-c", "Page C");

        builder.build(false); // first run: 3 pages

        // Modify page-b only
        writePage("page-b", "Page B Updated", "page-c", "page-a");

        var stats = builder.build(false);

        assertThat(stats.pagesProcessed()).isEqualTo(1);
        // page-a→b, page-b→c, page-b→a = 3 edges
        assertThat(stats.edges()).isEqualTo(3);
        var graph = graphStore.load();
        assertThat(graph.getEdge("page-b", "page-a")).isNotNull(); // new link from updated page-b
        assertThat(graph.getEdge("page-a", "page-b")).isNotNull(); // unchanged from page-a
    }

    @Test
    void forceTrueReprocessesAllPages() {
        writePage("page-a", "Page A", "page-b");
        writePage("page-b", "Page B");

        builder.build(false); // first run: 2 pages
        var stats = builder.build(true); // force

        assertThat(stats.pagesProcessed()).isEqualTo(2);
    }

    @Test
    void statsAreCorrect() {
        writePage("page-a", "Page A", "page-b", "page-c");
        writePage("page-b", "Page B");
        writePage("page-c", "Page C");

        var stats = builder.build(false);

        assertThat(stats.nodes()).isEqualTo(3);
        assertThat(stats.edges()).isEqualTo(2);
        assertThat(stats.pagesProcessed()).isEqualTo(3);
    }
}
