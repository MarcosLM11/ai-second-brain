package brain.server.mcp;

import brain.core.model.GraphAnalysis;
import brain.core.port.WikiStore;
import brain.graph.GraphAnalyzer;
import brain.graph.GraphBuilder;
import brain.graph.GraphReportWriter;
import brain.graph.GraphStoreSqlite;
import brain.graph.GraphTraversal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MCP tools for graph-based wiki traversal.
 */
@Component
public class GraphTools {

    private final WikiStore wikiStore;
    private final GraphBuilder graphBuilder;
    private final GraphTraversal graphTraversal;
    private final GraphAnalyzer graphAnalyzer;
    private final GraphStoreSqlite graphStore;
    private final GraphReportWriter graphReportWriter;

    @Value("${brain.graph.god-nodes-top-n:10}")
    int godNodesTopN;

    @Value("${brain.graph.community-count:5}")
    int communityCount;

    @Value("${brain.graph.surprise-min-conf:0.7}")
    double surpriseMinConf;

    @Value("${brain.wiki-root:~/brain/wiki}")
    private String wikiRootRaw;

    private Path wikiRoot;

    @PostConstruct
    void init() {
        wikiRoot = wikiRootRaw.startsWith("~/")
            ? Path.of(System.getProperty("user.home"), wikiRootRaw.substring(2))
            : Path.of(wikiRootRaw);
    }

    public GraphTools(WikiStore wikiStore, GraphBuilder graphBuilder, GraphTraversal graphTraversal,
                      GraphAnalyzer graphAnalyzer, GraphStoreSqlite graphStore, GraphReportWriter graphReportWriter) {
        this.wikiStore = wikiStore;
        this.graphBuilder = graphBuilder;
        this.graphTraversal = graphTraversal;
        this.graphAnalyzer = graphAnalyzer;
        this.graphStore = graphStore;
        this.graphReportWriter = graphReportWriter;
    }

    @Tool(description = """
        Build the knowledge graph from wiki wikilinks.
        Reads all wiki pages, extracts [[wikilinks]], and persists nodes and LINKS_TO edges in SQLite.
        Incremental by default: only reprocesses pages whose content has changed (SHA-256).
        Use force=true to reprocess all pages regardless of cache.
        Returns stats: nodes, edges, and pages processed.
        """)
    public String graph_build(
        @ToolParam(description = "If true, reprocess all pages ignoring the incremental cache") boolean force
    ) {
        var stats = graphBuilder.build(force);
        return "Graph built: %d nodes, %d edges, %d pages processed"
            .formatted(stats.nodes(), stats.edges(), stats.pagesProcessed());
    }

    @Tool(description = """
        Query the knowledge graph via BFS from a specific node.
        Returns a JSON subgraph containing nodes and edges within the given number of hops.
        hops is clamped to [1, 5]. Returns an error message string if the node does not exist.
        """)
    public String graph_query(
        @ToolParam(description = "Starting node ID for BFS traversal") String nodeId,
        @ToolParam(description = "Number of hops to traverse (1–5)") int hops
    ) {
        int clampedHops = Math.min(Math.max(hops, 1), 5);
        try {
            var subGraph = graphTraversal.bfs(nodeId, clampedHops);
            return new ObjectMapper().writeValueAsString(subGraph);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error querying graph: " + e.getMessage();
        }
    }

    @Tool(description = """
        Analyze the knowledge graph: computes god nodes (high betweenness centrality),
        detects communities (Girvan-Newman clustering), finds surprise cross-community connections,
        and builds an expertise map. Also writes GRAPH_REPORT.md to the wiki root.
        Returns a JSON representation of GraphAnalysis.
        Returns an error message if the graph has not been built yet (run graph_build first).
        """)
    public String graph_analyze() {
        try {
            GraphAnalysis analysis = graphAnalyzer.analyze(graphStore, godNodesTopN, communityCount, surpriseMinConf);
            graphReportWriter.write(analysis);
            return new ObjectMapper().writeValueAsString(analysis);
        } catch (IllegalStateException e) {
            return e.getMessage();
        } catch (Exception e) {
            return "Error analyzing graph: " + e.getMessage();
        }
    }

    @Tool(description = """
        Read the content of GRAPH_REPORT.md from the wiki root.
        Returns the markdown content of the last generated graph analysis report.
        Returns an error message if the report has not been generated yet (run graph_analyze first).
        """)
    public String graph_report_read() {
        try {
            Path reportPath = wikiRoot.resolve("GRAPH_REPORT.md");
            if (!Files.exists(reportPath)) {
                return "GRAPH_REPORT.md not found. Run graph_analyze first.";
            }
            return Files.readString(reportPath);
        } catch (Exception e) {
            return "Error reading graph report: " + e.getMessage();
        }
    }
}
