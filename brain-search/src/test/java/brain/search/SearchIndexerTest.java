package brain.search;

import brain.core.model.NodeType;
import brain.core.model.WikiPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchIndexerTest {

    @TempDir
    Path tempDir;

    SearchIndexer indexer;

    @BeforeEach
    void setUp() {
        indexer = new SearchIndexer(tempDir.resolve("search.db"));
    }

    // ─── indexAll ─────────────────────────────────────────────────────────────

    @Test
    void indexAllStoresOneRowPerPage() {
        List<WikiPage> pages = pages(
            page("jwt", "JWT Authentication"),
            page("oauth2", "OAuth 2.0"),
            page("rsa", "RSA Algorithm")
        );

        indexer.indexAll(pages);

        assertThat(indexer.countRows()).isEqualTo(3);
    }

    @Test
    void indexAllOnTenPagesCompletesWithoutError() {
        List<WikiPage> pages = pages(
            page("p1", "Page 1"), page("p2", "Page 2"), page("p3", "Page 3"),
            page("p4", "Page 4"), page("p5", "Page 5"), page("p6", "Page 6"),
            page("p7", "Page 7"), page("p8", "Page 8"), page("p9", "Page 9"),
            page("p10", "Page 10")
        );

        indexer.indexAll(pages);

        assertThat(indexer.countRows()).isEqualTo(10);
    }

    @Test
    void indexAllRebuildsFromScratch() {
        indexer.indexAll(pages(page("old", "Old Page")));
        assertThat(indexer.countRows()).isEqualTo(1);

        indexer.indexAll(pages(page("new1", "New Page 1"), page("new2", "New Page 2")));

        assertThat(indexer.countRows()).isEqualTo(2);
    }

    @Test
    void indexAllWithEmptyListClearsIndex() {
        indexer.indexAll(pages(page("page", "A Page")));

        indexer.indexAll(List.of());

        assertThat(indexer.countRows()).isEqualTo(0);
    }

    // ─── indexPage ────────────────────────────────────────────────────────────

    @Test
    void indexPageInsertsNewEntry() {
        indexer.indexPage(page("jwt", "JWT Authentication"));

        assertThat(indexer.countRows()).isEqualTo(1);
    }

    @Test
    void indexPageUpdatesExistingEntry() {
        indexer.indexPage(page("jwt", "JWT Authentication"));
        indexer.indexPage(page("jwt", "JWT Authentication Updated"));

        // Same page_id — still one row
        assertThat(indexer.countRows()).isEqualTo(1);
    }

    @Test
    void indexPageIncrementalUpdateDoesNotAffectOtherPages() {
        indexer.indexAll(pages(
            page("jwt", "JWT Authentication"),
            page("oauth2", "OAuth 2.0")
        ));

        indexer.indexPage(page("jwt", "JWT — Updated"));

        assertThat(indexer.countRows()).isEqualTo(2);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private WikiPage page(String id, String title) {
        return new WikiPage(id, Path.of(id + ".md"), NodeType.CONCEPT, title,
            List.of(), List.of("tag1", "tag2"), List.of(),
            Instant.now(), Instant.now(), "# " + title + "\n\nSome content about " + title);
    }

    @SafeVarargs
    private <T> List<T> pages(T... items) {
        return List.of(items);
    }
}
