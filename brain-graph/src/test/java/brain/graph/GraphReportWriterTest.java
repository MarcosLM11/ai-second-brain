package brain.graph;

import brain.core.model.Community;
import brain.core.model.GraphAnalysis;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import brain.core.model.SurpriseEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GraphReportWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writeCreatesGraphReportFile() throws IOException {
        var writer = new GraphReportWriter(tempDir);
        GraphAnalysis analysis = buildSampleAnalysis();

        Path reportPath = writer.write(analysis);

        assertThat(reportPath).exists();
        assertThat(reportPath).isEqualTo(tempDir.resolve("GRAPH_REPORT.md"));
    }

    @Test
    void reportContainsAllSections() throws IOException {
        var writer = new GraphReportWriter(tempDir);
        GraphAnalysis analysis = buildSampleAnalysis();

        writer.write(analysis);
        String content = Files.readString(tempDir.resolve("GRAPH_REPORT.md"));

        assertThat(content).contains("## God Nodes");
        assertThat(content).contains("## Communities");
        assertThat(content).contains("## Surprise Connections");
        assertThat(content).contains("## Expertise Map");
    }

    @Test
    void reportContainsGodNodeData() throws IOException {
        var writer = new GraphReportWriter(tempDir);
        GraphAnalysis analysis = buildSampleAnalysis();

        writer.write(analysis);
        String content = Files.readString(tempDir.resolve("GRAPH_REPORT.md"));

        assertThat(content).contains("hub");
        assertThat(content).contains("Hub Node");
    }

    @Test
    void reportContainsCommunityData() throws IOException {
        var writer = new GraphReportWriter(tempDir);
        GraphAnalysis analysis = buildSampleAnalysis();

        writer.write(analysis);
        String content = Files.readString(tempDir.resolve("GRAPH_REPORT.md"));

        assertThat(content).contains("Community 0");
        assertThat(content).contains("Community 1");
    }

    @Test
    void reportContainsSurpriseEdgeData() throws IOException {
        var writer = new GraphReportWriter(tempDir);
        GraphAnalysis analysis = buildSampleAnalysis();

        writer.write(analysis);
        String content = Files.readString(tempDir.resolve("GRAPH_REPORT.md"));

        assertThat(content).contains("node-a");
        assertThat(content).contains("node-b");
    }

    private GraphAnalysis buildSampleAnalysis() {
        Instant now = Instant.now();
        GraphNode hub = new GraphNode("hub", "Hub Node", NodeType.CONCEPT, 0, now, now);

        List<GraphNode> godNodes = List.of(hub);
        List<Community> communities = List.of(
            new Community(0, Set.of("hub", "a")),
            new Community(1, Set.of("b", "c"))
        );
        List<SurpriseEdge> surprises = List.of(
            new SurpriseEdge("node-a", "node-b", 0.85)
        );
        Map<String, Double> expertiseMap = Map.of("hub", 0.9, "a", 0.3, "b", 0.1);

        return new GraphAnalysis(godNodes, communities, surprises, expertiseMap);
    }
}
