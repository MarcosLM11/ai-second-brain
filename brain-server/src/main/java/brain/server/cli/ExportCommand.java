package brain.server.cli;

import brain.core.config.BrainConfig;
import brain.core.config.BrainConfigLoader;
import brain.core.config.ModelConfig;
import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import brain.graph.GraphStoreSqlite;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI command that exports the knowledge graph to GraphML or JSON.
 *
 * <p>Usage:
 * <pre>
 *   brain export --format=graphml --output=brain_graph.graphml
 *   brain export --format=json    --output=brain_graph.json
 * </pre>
 */
@Command(
    name = "export",
    description = "Export the knowledge graph (GraphML or JSON)",
    mixinStandardHelpOptions = true
)
public class ExportCommand implements Runnable {

    @ParentCommand
    BrainCli parent;

    @Option(
        names = {"--format", "-f"},
        description = "Export format: graphml | json (default: graphml)",
        defaultValue = "graphml"
    )
    String format;

    @Option(
        names = {"--output", "-o"},
        description = "Output file path",
        required = true
    )
    Path output;

    @Override
    public void run() {
        try {
            BrainConfig config = loadConfig();
            GraphStoreSqlite store = new GraphStoreSqlite(config.graphDbPath());
            Graph<String, DefaultWeightedEdge> graph = store.load();
            List<GraphEdge> edges = store.loadEdges();

            String content = switch (format.toLowerCase()) {
                case "graphml" -> exportGraphML(graph, store);
                case "json"    -> exportJson(graph, store, edges);
                default -> {
                    System.err.println("[brain export] Unknown format: " + format + ". Use graphml or json.");
                    yield null;
                }
            };

            if (content == null) return;

            Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent());
            Files.writeString(output, content);
            System.out.printf("[brain export] Exported %d nodes to %s%n", graph.vertexSet().size(), output);

        } catch (Exception e) {
            System.err.println("[brain export] ERROR: " + e.getMessage());
        }
    }

    private String exportGraphML(Graph<String, DefaultWeightedEdge> graph, GraphStoreSqlite store) {
        GraphMLExporter<String, DefaultWeightedEdge> exporter = new GraphMLExporter<>();
        exporter.setExportEdgeWeights(true);

        // Node attributes
        exporter.registerAttribute("label",     GraphMLExporter.AttributeCategory.NODE, org.jgrapht.nio.AttributeType.STRING);
        exporter.registerAttribute("type",      GraphMLExporter.AttributeCategory.NODE, org.jgrapht.nio.AttributeType.STRING);
        exporter.registerAttribute("community", GraphMLExporter.AttributeCategory.NODE, org.jgrapht.nio.AttributeType.INT);

        exporter.setVertexAttributeProvider(nodeId -> {
            Map<String, Attribute> attrs = new LinkedHashMap<>();
            store.getNode(nodeId).ifPresentOrElse(
                node -> {
                    attrs.put("label",     DefaultAttribute.createAttribute(node.label()));
                    attrs.put("type",      DefaultAttribute.createAttribute(node.type().name()));
                    attrs.put("community", DefaultAttribute.createAttribute(node.community()));
                },
                () -> {
                    attrs.put("label",     DefaultAttribute.createAttribute(nodeId));
                    attrs.put("type",      DefaultAttribute.createAttribute(NodeType.CONCEPT.name()));
                    attrs.put("community", DefaultAttribute.createAttribute(-1));
                }
            );
            return attrs;
        });

        StringWriter sw = new StringWriter();
        exporter.exportGraph(graph, new PrintWriter(sw));
        return sw.toString();
    }

    private String exportJson(
            Graph<String, DefaultWeightedEdge> graph,
            GraphStoreSqlite store,
            List<GraphEdge> edges) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        ArrayNode nodesArray = mapper.createArrayNode();
        for (String nodeId : graph.vertexSet()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("id", nodeId);
            store.getNode(nodeId).ifPresentOrElse(
                node -> {
                    n.put("label",     node.label());
                    n.put("type",      node.type().name());
                    n.put("community", node.community());
                },
                () -> {
                    n.put("label",     nodeId);
                    n.put("type",      NodeType.CONCEPT.name());
                    n.put("community", -1);
                }
            );
            nodesArray.add(n);
        }
        root.set("nodes", nodesArray);

        ArrayNode edgesArray = mapper.createArrayNode();
        for (GraphEdge edge : edges) {
            ObjectNode e = mapper.createObjectNode();
            e.put("from",       edge.from());
            e.put("to",         edge.to());
            e.put("type",       edge.type().name());
            e.put("origin",     edge.origin().name());
            e.put("confidence", edge.confidence());
            edgesArray.add(e);
        }
        root.set("edges", edgesArray);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private BrainConfig loadConfig() throws Exception {
        if (parent != null && parent.configFile != null) {
            return BrainConfigLoader.load(parent.configFile);
        }
        Path home = Path.of(System.getProperty("user.home"), "brain", "brain.toml");
        if (home.toFile().exists()) {
            return BrainConfigLoader.load(home);
        }
        return new BrainConfig(
            Path.of(System.getProperty("user.home"), "brain", "wiki"),
            Path.of(System.getProperty("user.home"), "brain", "raw"),
            Path.of(System.getProperty("user.home"), "brain", "brain_graph.db"),
            Path.of(System.getProperty("user.home"), "brain", "SCHEMA.md"),
            "UTC", 2000, 3, 10, 5, 0.7, true, 500,
            new ModelConfig(
                "claude-haiku-4-5-20251001",
                "claude-sonnet-4-6",
                "claude-sonnet-4-6",
                "claude-haiku-4-5-20251001"
            )
        );
    }
}
