package brain.server.mcp;

import brain.wiki.LintReportWriter;
import brain.wiki.LintService;
import brain.wiki.LintService.BrokenLink;
import brain.wiki.LintService.StructuralReport;
import brain.wiki.LogAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LintTools {

    private final LintService lintService;
    private final LintReportWriter lintReportWriter;
    private final LogAppender logAppender;

    @Value("${brain.wiki-root:~/brain/wiki}")
    private String wikiRootRaw;

    private Path wikiRoot;

    @PostConstruct
    void init() {
        wikiRoot = wikiRootRaw.startsWith("~/")
            ? Path.of(System.getProperty("user.home"), wikiRootRaw.substring(2))
            : Path.of(wikiRootRaw);
    }

    public LintTools(LintService lintService, LintReportWriter lintReportWriter, LogAppender logAppender) {
        this.lintService = lintService;
        this.lintReportWriter = lintReportWriter;
        this.logAppender = logAppender;
    }

    @Tool(description = """
        Find orphaned wiki pages: pages that have no incoming wikilinks from any other page.
        index.md and log.md are always excluded from results.
        Self-referencing links do not count as incoming links.
        Returns a list of orphaned page IDs, or a message if none are found.
        """)
    public String lint_orphans() {
        List<String> orphans = lintService.findOrphans(wikiRoot);
        if (orphans.isEmpty()) return "No orphaned pages found.";
        return "Orphaned pages (%d):\n".formatted(orphans.size())
            + orphans.stream().sorted()
                .map(id -> "  - " + id)
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = """
        Find broken wikilinks: [[references]] that point to pages that do not exist in the wiki.
        Returns a list of broken links with their source page and missing target,
        or a message if none are found.
        """)
    public String lint_broken_links() {
        List<BrokenLink> broken = lintService.findBrokenLinks(wikiRoot);
        if (broken.isEmpty()) return "No broken wikilinks found.";
        return "Broken wikilinks (%d):\n".formatted(broken.size())
            + broken.stream()
                .sorted(Comparator.comparing(BrokenLink::sourcePageId).thenComparing(BrokenLink::target))
                .map(bl -> "  - [[%s]] in %s".formatted(bl.target(), bl.sourcePageId()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = """
        Run a full structural lint on the wiki. Detects:
        - orphaned pages (no incoming wikilinks from other pages)
        - broken wikilinks ([[references]] pointing to non-existent pages)
        - asymmetric backlinks (A links to B but B does not link back to A)
        Writes HEALTH_REPORT.md to the wiki root and appends an entry to log.md.
        Returns a JSON object: {orphans: [], brokenLinks: [{sourcePageId, target}], asymmetricLinks: [{from, to}]}
        """)
    public String lint_structural() {
        try {
            StructuralReport report = lintService.buildStructuralReport(wikiRoot);
            lintReportWriter.write(report);
            logAppender.append("lint_structural: %d orphans, %d broken links, %d asymmetric backlinks"
                .formatted(report.orphans().size(), report.brokenLinks().size(), report.asymmetricLinks().size()));
            return new ObjectMapper().writeValueAsString(report);
        } catch (Exception e) {
            return "Error running structural lint: " + e.getMessage();
        }
    }

}
