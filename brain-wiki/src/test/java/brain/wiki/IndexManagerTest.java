package brain.wiki;

import brain.core.model.NodeType;
import brain.core.model.WikiPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexManagerTest {

    @TempDir
    Path wikiRoot;

    IndexManager manager;

    @BeforeEach
    void setUp() {
        manager = new IndexManager(wikiRoot);
    }

    // --- addOrUpdate ---

    @Test
    void addOrUpdateCreatesIndexWhenMissing() throws IOException {
        manager.addOrUpdate(page("concepts/jwt", "JWT", NodeType.CONCEPT, "---\ntitle: JWT\n---\nA compact token."));

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).contains("| concepts/jwt |");
        assertThat(content).contains("JWT");
        assertThat(content).contains("CONCEPT");
        assertThat(content).contains("A compact token.");
    }

    @Test
    void addOrUpdateAppendsNewEntry() throws IOException {
        manager.addOrUpdate(page("concepts/jwt",   "JWT",   NodeType.CONCEPT, "# JWT\nA token."));
        manager.addOrUpdate(page("entities/alice", "Alice", NodeType.ENTITY,  "# Alice\nA user."));

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).contains("| concepts/jwt |");
        assertThat(content).contains("| entities/alice |");
    }

    @Test
    void addOrUpdateReplacesExistingEntryWithoutDuplicates() throws IOException {
        manager.addOrUpdate(page("concepts/jwt", "JWT Old", NodeType.CONCEPT, "# JWT\nOld summary."));
        manager.addOrUpdate(page("concepts/jwt", "JWT New", NodeType.CONCEPT, "# JWT\nNew summary."));

        String content = Files.readString(wikiRoot.resolve("index.md"));
        long occurrences = content.lines()
            .filter(l -> l.contains("concepts/jwt"))
            .count();
        assertThat(occurrences).isEqualTo(1);
        assertThat(content).contains("JWT New");
        assertThat(content).doesNotContain("JWT Old");
    }

    // --- remove ---

    @Test
    void removeDeletesExistingEntry() throws IOException {
        manager.addOrUpdate(page("concepts/jwt",   "JWT",   NodeType.CONCEPT, "# JWT\nA token."));
        manager.addOrUpdate(page("entities/alice", "Alice", NodeType.ENTITY,  "# Alice\nA user."));

        manager.remove("concepts/jwt");

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).doesNotContain("concepts/jwt");
        assertThat(content).contains("entities/alice");
    }

    @Test
    void removeIsNoopWhenPageNotInIndex() {
        manager.addOrUpdate(page("concepts/jwt", "JWT", NodeType.CONCEPT, "# JWT\nA token."));
        manager.remove("nonexistent/page"); // must not throw
    }

    @Test
    void removeOnMissingIndexFileIsNoop() {
        manager.remove("concepts/jwt"); // must not throw
    }

    // --- rebuild ---

    @Test
    void rebuildGeneratesFullIndexFromPageList() throws IOException {
        List<WikiPage> pages = List.of(
            page("concepts/jwt",   "JWT",   NodeType.CONCEPT,  "# JWT\nA token."),
            page("entities/alice", "Alice", NodeType.ENTITY,   "# Alice\nA user."),
            page("decisions/adr1", "ADR-1", NodeType.DECISION, "# ADR-1\nUse JWT.")
        );

        manager.rebuild(pages);

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).contains("concepts/jwt");
        assertThat(content).contains("entities/alice");
        assertThat(content).contains("decisions/adr1");
    }

    @Test
    void rebuildReplacesExistingIndex() throws IOException {
        manager.addOrUpdate(page("concepts/old", "Old", NodeType.CONCEPT, "# Old\nOld."));

        manager.rebuild(List.of(page("concepts/new", "New", NodeType.CONCEPT, "# New\nNew.")));

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).contains("concepts/new");
        assertThat(content).doesNotContain("concepts/old");
    }

    @Test
    void rebuildWithEmptyListProducesEmptyTable() throws IOException {
        manager.rebuild(List.of());

        String content = Files.readString(wikiRoot.resolve("index.md"));
        assertThat(content).contains("| page | title | type | summary |");
        long dataRows = content.lines()
            .filter(l -> l.startsWith("| ") && !l.startsWith("| page") && !l.startsWith("|---"))
            .count();
        assertThat(dataRows).isZero();
    }

    // --- summary extraction ---

    @Test
    void addOrUpdateExtractsSummarySkippingFrontmatter() throws IOException {
        String content = """
                ---
                title: JWT
                type: concept
                ---
                # JWT Heading

                A compact token format.
                """;
        manager.addOrUpdate(page("concepts/jwt", "JWT", NodeType.CONCEPT, content));

        assertThat(Files.readString(wikiRoot.resolve("index.md")))
            .contains("A compact token format.");
    }

    @Test
    void addOrUpdateTruncatesSummaryAt80Characters() throws IOException {
        String longBody = "A".repeat(100);
        manager.addOrUpdate(page("concepts/jwt", "JWT", NodeType.CONCEPT, longBody));

        String content = Files.readString(wikiRoot.resolve("index.md"));
        String row = content.lines().filter(l -> l.contains("concepts/jwt")).findFirst().orElseThrow();
        String[] cols = row.split("\\|");
        String summary = cols[4].trim();
        assertThat(summary.length()).isLessThanOrEqualTo(80);
        assertThat(summary).endsWith("...");
    }

    // --- helpers ---

    private WikiPage page(String id, String title, NodeType type, String content) {
        return new WikiPage(id, wikiRoot.resolve(id + ".md"), type, title,
            List.of(), List.of(), List.of(), Instant.EPOCH, Instant.EPOCH, content);
    }
}
