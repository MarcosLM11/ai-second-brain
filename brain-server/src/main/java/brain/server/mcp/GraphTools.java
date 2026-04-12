package brain.server.mcp;

import brain.core.model.WikiPage;
import brain.core.port.WikiStore;
import brain.graph.GraphBuilder;
import brain.graph.GraphTraversal;
import brain.wiki.WikilinkExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.*;

/**
 * MCP tools for graph-based wiki traversal.
 */
@Component
public class GraphTools {

    private final WikiStore wikiStore;
    private final GraphBuilder graphBuilder;
    private final GraphTraversal graphTraversal;

    public GraphTools(WikiStore wikiStore, GraphBuilder graphBuilder, GraphTraversal graphTraversal) {
        this.wikiStore = wikiStore;
        this.graphBuilder = graphBuilder;
        this.graphTraversal = graphTraversal;
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
}
