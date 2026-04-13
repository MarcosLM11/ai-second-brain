package brain.server.cli;

import brain.wiki.LintReportWriter;
import brain.wiki.LintService;
import brain.wiki.LintService.StructuralReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLI integration tests using real filesystem fixtures.
 * These tests do NOT require ANTHROPIC_API_KEY; they test structural CLI operations.
 * Tests that need the API key are gated with {@code @EnabledIfEnvironmentVariable}.
 */
class CliIT {

    @TempDir
    Path wikiDir;

    @Test
    void lintStructuralOnCleanWikiReportsZeroIssues() throws IOException {
        // Set up a clean wiki with two interlinked pages
        Files.writeString(wikiDir.resolve("index.md"), """
            ---
            title: Index
            type: concept
            tags: []
            sources: []
            created: 2026-01-01
            updated: 2026-01-01
            ---
            # Index
            See [[concepts/spring-ai]].
            """);
        Files.createDirectories(wikiDir.resolve("concepts"));
        Files.writeString(wikiDir.resolve("concepts/spring-ai.md"), """
            ---
            title: Spring AI
            type: concept
            tags: [spring, ai]
            sources: []
            created: 2026-01-01
            updated: 2026-01-01
            ---
            # Spring AI
            Framework for AI integration. See [[index]].
            """);

        LintService lintService = new LintService();
        StructuralReport report = lintService.buildStructuralReport(wikiDir);

        assertThat(report.orphans()).isEmpty();
        assertThat(report.brokenLinks()).isEmpty();
    }

    @Test
    void lintStructuralDetectsBrokenLink() throws IOException {
        Files.writeString(wikiDir.resolve("index.md"), """
            ---
            title: Index
            type: concept
            tags: []
            sources: []
            created: 2026-01-01
            updated: 2026-01-01
            ---
            # Index
            See [[nonexistent-page]].
            """);

        LintService lintService = new LintService();
        StructuralReport report = lintService.buildStructuralReport(wikiDir);

        assertThat(report.brokenLinks()).isNotEmpty();
        assertThat(report.brokenLinks())
            .anyMatch(bl -> bl.target().equals("nonexistent-page"));
    }

    @Test
    void lintStructuralDetectsOrphanPage() throws IOException {
        Files.writeString(wikiDir.resolve("index.md"), """
            ---
            title: Index
            type: concept
            tags: []
            sources: []
            created: 2026-01-01
            updated: 2026-01-01
            ---
            # Index
            No links here.
            """);
        Files.createDirectories(wikiDir.resolve("concepts"));
        Files.writeString(wikiDir.resolve("concepts/orphan.md"), """
            ---
            title: Orphan
            type: concept
            tags: []
            sources: []
            created: 2026-01-01
            updated: 2026-01-01
            ---
            # Orphan
            Nobody links to me.
            """);

        LintService lintService = new LintService();
        StructuralReport report = lintService.buildStructuralReport(wikiDir);

        assertThat(report.orphans()).contains("concepts/orphan");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void lintReportWriterGeneratesHealthReport() throws IOException {
        Files.writeString(wikiDir.resolve("index.md"), "# Index\n");

        LintService lintService = new LintService();
        StructuralReport report = lintService.buildStructuralReport(wikiDir);

        LintReportWriter writer = new LintReportWriter(wikiDir);
        writer.write(report);

        Path healthReport = wikiDir.resolve("HEALTH_REPORT.md");
        assertThat(healthReport).exists();
        assertThat(Files.readString(healthReport))
            .contains("Health Report")
            .contains("Structural");
    }
}
