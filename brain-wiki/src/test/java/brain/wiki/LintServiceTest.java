package brain.wiki;

import brain.wiki.LintService.BrokenLink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LintServiceTest {

    @TempDir
    Path wikiRoot;

    LintService service = new LintService();

    // ─── findOrphans ───────────────────────────────────────────────────────────

    @Test
    void detectsOrphanPage() throws IOException {
        write("connected.md", "# Connected\n\nSee [[orphan]] for nothing.");
        write("orphan.md", "# Orphan\n\nNo one links here.");

        // Wait — connected.md links to orphan, so orphan is NOT orphaned.
        // Let's use a truly isolated page:
        write("isolated.md", "# Isolated\n\nNo incoming links.");

        List<String> orphans = service.findOrphans(wikiRoot);

        assertThat(orphans).contains("isolated");
        assertThat(orphans).doesNotContain("orphan"); // linked from connected.md
    }

    @Test
    void knownOrphanIsDetected() throws IOException {
        write("page-a.md", "# A\n\n[[page-b]]");
        write("page-b.md", "# B\n\n[[page-a]]"); // mutual links — neither is orphaned
        write("lonely.md", "# Lonely\n\nNo incoming links at all.");

        List<String> orphans = service.findOrphans(wikiRoot);

        assertThat(orphans).containsExactly("lonely");
    }

    @Test
    void pageWithNoOutgoingLinksIsNotConfusedWithOrphan() throws IOException {
        write("linker.md", "# Linker\n\n[[no-outgoing]]");
        write("no-outgoing.md", "# No Outgoing\n\nJust text, no wikilinks.");

        List<String> orphans = service.findOrphans(wikiRoot);

        // no-outgoing has no outgoing links but IS referenced by linker — not an orphan
        assertThat(orphans).doesNotContain("no-outgoing");
        // linker has no incoming links — it IS an orphan
        assertThat(orphans).contains("linker");
    }

    @Test
    void indexMdIsNeverReportedAsOrphan() throws IOException {
        write("index.md", "# Index\n\nNo one links to me.");

        List<String> orphans = service.findOrphans(wikiRoot);

        assertThat(orphans).doesNotContain("index");
    }

    @Test
    void selfReferenceDoesNotPreventOrphanDetection() throws IOException {
        write("self-ref.md", "# Self\n\n[[self-ref]] is a self-link.");

        List<String> orphans = service.findOrphans(wikiRoot);

        assertThat(orphans).contains("self-ref");
    }

    @Test
    void returnsEmptyWhenNoOrphans() throws IOException {
        write("a.md", "[[b]]");
        write("b.md", "[[a]]");

        List<String> orphans = service.findOrphans(wikiRoot);

        assertThat(orphans).isEmpty();
    }

    // ─── findBrokenLinks ───────────────────────────────────────────────────────

    @Test
    void detectsBrokenWikilink() throws IOException {
        write("page.md", "# Page\n\nSee [[ghost]] for details.");

        List<BrokenLink> broken = service.findBrokenLinks(wikiRoot);

        assertThat(broken).hasSize(1);
        assertThat(broken.getFirst().sourcePageId()).isEqualTo("page");
        assertThat(broken.getFirst().target()).isEqualTo("ghost");
    }

    @Test
    void knownBrokenWikilinkIsDetected() throws IOException {
        write("existing.md", "# Existing\n\nContent.");
        write("source.md", "# Source\n\n[[existing]] and [[non-existent-page]]");

        List<BrokenLink> broken = service.findBrokenLinks(wikiRoot);

        assertThat(broken).hasSize(1);
        assertThat(broken.getFirst().sourcePageId()).isEqualTo("source");
        assertThat(broken.getFirst().target()).isEqualTo("non-existent-page");
    }

    @Test
    void validWikilinkIsNotReportedAsBroken() throws IOException {
        write("target.md", "# Target\n\nContent.");
        write("source.md", "# Source\n\n[[target]]");

        List<BrokenLink> broken = service.findBrokenLinks(wikiRoot);

        assertThat(broken).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoBrokenLinks() throws IOException {
        write("a.md", "[[b]]");
        write("b.md", "[[a]]");

        List<BrokenLink> broken = service.findBrokenLinks(wikiRoot);

        assertThat(broken).isEmpty();
    }

    @Test
    void pagesWithNoOutgoingLinksProduceNoBrokenLinks() throws IOException {
        write("plain.md", "# Plain\n\nJust text.");

        List<BrokenLink> broken = service.findBrokenLinks(wikiRoot);

        assertThat(broken).isEmpty();
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private void write(String relativePath, String content) throws IOException {
        Path target = wikiRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }
}
