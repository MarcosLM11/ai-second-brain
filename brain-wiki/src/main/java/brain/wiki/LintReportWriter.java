package brain.wiki;

import brain.wiki.LintService.AsymmetricLink;
import brain.wiki.LintService.BrokenLink;
import brain.wiki.LintService.StructuralReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/**
 * Writes {@code HEALTH_REPORT.md} to the wiki root from a {@link StructuralReport}.
 */
public class LintReportWriter {

    private final Path wikiRoot;

    public LintReportWriter(Path wikiRoot) {
        this.wikiRoot = wikiRoot.toAbsolutePath().normalize();
    }

    public Path write(StructuralReport report) throws IOException {
        Path reportPath = wikiRoot.resolve("HEALTH_REPORT.md");
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, buildMarkdown(report));
        return reportPath;
    }

    private String buildMarkdown(StructuralReport report) {
        var sb = new StringBuilder();
        sb.append("# Health Report\n\n");
        sb.append("_Generated at: ").append(Instant.now()).append("_\n\n");

        // Orphans
        sb.append("## Orphaned Pages\n\n");
        if (report.orphans().isEmpty()) {
            sb.append("No orphaned pages.\n\n");
        } else {
            for (String id : report.orphans().stream().sorted().toList()) {
                sb.append("- ").append(id).append("\n");
            }
            sb.append("\n");
        }

        // Broken links
        sb.append("## Broken Wikilinks\n\n");
        if (report.brokenLinks().isEmpty()) {
            sb.append("No broken wikilinks.\n\n");
        } else {
            report.brokenLinks().stream()
                .sorted(java.util.Comparator.comparing(BrokenLink::sourcePageId)
                    .thenComparing(BrokenLink::target))
                .forEach(bl -> sb.append("- `[[").append(bl.target()).append("]]` in ")
                    .append(bl.sourcePageId()).append("\n"));
            sb.append("\n");
        }

        // Asymmetric links
        sb.append("## Asymmetric Backlinks\n\n");
        if (report.asymmetricLinks().isEmpty()) {
            sb.append("No asymmetric backlinks.\n\n");
        } else {
            report.asymmetricLinks().stream()
                .sorted(java.util.Comparator.comparing(AsymmetricLink::from)
                    .thenComparing(AsymmetricLink::to))
                .forEach(al -> sb.append("- ").append(al.from())
                    .append(" → ").append(al.to()).append(" (no backlink)\n"));
            sb.append("\n");
        }

        return sb.toString();
    }
}
