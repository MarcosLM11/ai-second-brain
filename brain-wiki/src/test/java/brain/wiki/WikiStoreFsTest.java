package brain.wiki;

import brain.core.model.NodeType;
import brain.core.model.WikiPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WikiStoreFsTest {

    @TempDir
    Path wikiRoot;

    WikiStoreFs store;

    @BeforeEach
    void setUp() {
        store = new WikiStoreFs(wikiRoot);
    }

    // --- read ---

    @Test
    void readReturnsEmptyWhenPageNotFound() {
        assertThat(store.read("concepts/nonexistent")).isEmpty();
    }

    // --- round-trip ---

    @Test
    void roundTripWriteThenRead() {
        String content = """
                ---
                title: "JWT Verification"
                type: concept
                aliases:
                  - JWT
                tags:
                  - security
                sources:
                  - sources/rfc7519
                created: 2024-01-15
                updated: 2024-03-20
                ---
                # JWT Verification

                A JWT is a compact token.
                """;

        store.write("concepts/jwt-verification", content);
        WikiPage page = store.read("concepts/jwt-verification").orElseThrow();

        assertThat(page.id()).isEqualTo("concepts/jwt-verification");
        assertThat(page.title()).isEqualTo("JWT Verification");
        assertThat(page.type()).isEqualTo(NodeType.CONCEPT);
        assertThat(page.aliases()).containsExactly("JWT");
        assertThat(page.tags()).containsExactly("security");
        assertThat(page.sources()).containsExactly("sources/rfc7519");
        assertThat(page.created()).isEqualTo(LocalDate.of(2024, 1, 15).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(page.updated()).isEqualTo(LocalDate.of(2024, 3, 20).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThat(page.content()).isEqualTo(content);
    }

    // --- write ---

    @Test
    void writeCreatesParentDirectoriesAutomatically() {
        store.write("deep/nested/dir/page", "# Deep Page");

        assertThat(wikiRoot.resolve("deep/nested/dir/page.md")).exists();
    }

    @Test
    void writeOverwritesExistingPage() {
        store.write("concepts/page", "# Original");
        store.write("concepts/page", "# Updated");

        assertThat(store.read("concepts/page").orElseThrow().content()).isEqualTo("# Updated");
    }

    // --- list ---

    @Test
    void listByTypeReturnsOnlyMatchingPages() {
        store.write("concepts/jwt",    "---\ntype: concept\n---\n# JWT");
        store.write("entities/alice",  "---\ntype: entity\n---\n# Alice");
        store.write("decisions/use-jwt", "---\ntype: decision\n---\n# Use JWT");

        List<WikiPage> concepts = store.list("concept", null);

        assertThat(concepts).hasSize(1);
        assertThat(concepts.get(0).id()).isEqualTo("concepts/jwt");
    }

    @Test
    void listWithNullTypeAndPatternReturnsAll() {
        store.write("concepts/a", "---\ntype: concept\n---\n# A");
        store.write("entities/b", "---\ntype: entity\n---\n# B");

        assertThat(store.list(null, null)).hasSize(2);
    }

    @Test
    void listByGlobPatternFiltersCorrectly() {
        store.write("concepts/jwt",    "---\ntype: concept\n---\n# JWT");
        store.write("concepts/oauth",  "---\ntype: concept\n---\n# OAuth");
        store.write("entities/alice",  "---\ntype: entity\n---\n# Alice");

        List<WikiPage> result = store.list(null, "concepts/*");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(WikiPage::id)
            .containsExactlyInAnyOrder("concepts/jwt", "concepts/oauth");
    }

    @Test
    void listAllExcludesIndexAndLog() throws IOException {
        store.write("concepts/page", "---\ntype: concept\n---\n# Page");
        Files.writeString(wikiRoot.resolve("index.md"), "# Index");
        Files.writeString(wikiRoot.resolve("log.md"), "# Log");

        List<WikiPage> pages = store.listAll();

        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).id()).isEqualTo("concepts/page");
    }

    @Test
    void listAllReturnsEmptyWhenWikiRootMissing() {
        WikiStoreFs storeOnMissingRoot = new WikiStoreFs(wikiRoot.resolve("does-not-exist"));

        assertThat(storeOnMissingRoot.listAll()).isEmpty();
    }

    // --- readRaw ---

    @Test
    void readRawReadsAnyFileUnderWikiRoot() throws IOException {
        Files.writeString(wikiRoot.resolve("index.md"), "# Index content");

        assertThat(store.readRaw("index.md")).hasValue("# Index content");
    }

    @Test
    void readRawReturnsEmptyWhenFileNotFound() {
        assertThat(store.readRaw("nonexistent.md")).isEmpty();
    }

    // --- path traversal (RNF-SEC-01) ---

    @Test
    void readThrowsSecurityExceptionOnPathTraversal() {
        assertThatThrownBy(() -> store.read("../../../etc/passwd"))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void writeThrowsSecurityExceptionOnPathTraversal() {
        assertThatThrownBy(() -> store.write("../../outside", "content"))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void readRawThrowsSecurityExceptionOnPathTraversal() {
        assertThatThrownBy(() -> store.readRaw("../../../etc/passwd"))
            .isInstanceOf(SecurityException.class);
    }
}
