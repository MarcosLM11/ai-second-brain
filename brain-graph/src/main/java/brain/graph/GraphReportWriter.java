package brain.graph;

import brain.core.model.Community;
import brain.core.model.GraphAnalysis;
import brain.core.model.GraphNode;
import brain.core.model.SurpriseEdge;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

public class GraphReportWriter {

    private final Path wikiRoot;

    public GraphReportWriter(Path wikiRoot) {
        this.wikiRoot = wikiRoot;
    }

    public Path getWikiRoot() {
        return wikiRoot;
    }

    public Path write(GraphAnalysis analysis) throws IOException {
        Path reportPath = wikiRoot.resolve("GRAPH_REPORT.md");
        String content = buildMarkdown(analysis);
        Files.writeString(reportPath, content);
        return reportPath;
    }

    private String buildMarkdown(GraphAnalysis analysis) {
        var sb = new StringBuilder();
        sb.append("# Graph Analysis Report\n\n");
        sb.append("_Generated at: ").append(Instant.now()).append("_\n\n");

        // God Nodes table
        sb.append("## God Nodes\n\n");
        sb.append("| Node | Label | Type | Betweenness Score |\n");
        sb.append("|------|-------|------|-------------------|\n");
        for (GraphNode node : analysis.godNodes()) {
            double score = analysis.expertiseMap().getOrDefault(node.id(), 0.0);
            sb.append("| ").append(node.id())
              .append(" | ").append(node.label())
              .append(" | ").append(node.type())
              .append(" | ").append(String.format("%.4f", score))
              .append(" |\n");
        }
        sb.append("\n");

        // Communities
        sb.append("## Communities\n\n");
        for (Community c : analysis.communities()) {
            sb.append("- **Community ").append(c.id()).append("**: ")
              .append(String.join(", ", c.nodeIds()))
              .append("\n");
        }
        sb.append("\n");

        // Surprise connections table
        sb.append("## Surprise Connections\n\n");
        sb.append("| From | To | Confidence |\n");
        sb.append("|------|----|------------|\n");
        for (SurpriseEdge edge : analysis.surprises()) {
            sb.append("| ").append(edge.from())
              .append(" | ").append(edge.to())
              .append(" | ").append(String.format("%.4f", edge.weight()))
              .append(" |\n");
        }
        sb.append("\n");

        // Expertise map (top 20, sorted by score desc)
        sb.append("## Expertise Map\n\n");
        sb.append("| Node | Score |\n");
        sb.append("|------|-------|\n");
        analysis.expertiseMap().entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(20)
            .forEach(e -> sb.append("| ").append(e.getKey())
                .append(" | ").append(String.format("%.4f", e.getValue()))
                .append(" |\n"));

        return sb.toString();
    }
}
