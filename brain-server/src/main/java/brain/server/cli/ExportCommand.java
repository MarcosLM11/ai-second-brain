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
 * CLI command that exports the knowledge graph to GraphML, JSON, or HTML.
 *
 * <p>Usage:
 * <pre>
 *   brain export --format=graphml --output=brain_graph.graphml
 *   brain export --format=json    --output=brain_graph.json
 *   brain export --format=html    --output=brain_graph.html
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
        description = "Export format: graphml | json | html (default: graphml)",
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
                case "html"    -> exportHtml(graph, store, edges);
                default -> {
                    System.err.println("[brain export] Unknown format: " + format + ". Use graphml, json, or html.");
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

    private String exportHtml(
            Graph<String, DefaultWeightedEdge> graph,
            GraphStoreSqlite store,
            List<GraphEdge> edges) throws Exception {

        // Build JSON data for the graph
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode data = mapper.createObjectNode();
        ArrayNode nodesArray = mapper.createArrayNode();
        for (String nodeId : graph.vertexSet()) {
            ObjectNode n = mapper.createObjectNode();
            n.put("id",    escapeJson(nodeId));
            store.getNode(nodeId).ifPresentOrElse(
                node -> {
                    n.put("label",     escapeJson(node.label()));
                    n.put("type",      escapeJson(node.type().name()));
                    n.put("community", node.community());
                },
                () -> {
                    n.put("label",     escapeJson(nodeId));
                    n.put("type",      escapeJson(NodeType.CONCEPT.name()));
                    n.put("community", -1);
                }
            );
            nodesArray.add(n);
        }
        data.set("nodes", nodesArray);

        ArrayNode edgesArray = mapper.createArrayNode();
        for (GraphEdge edge : edges) {
            ObjectNode e = mapper.createObjectNode();
            e.put("source",     escapeJson(edge.from()));
            e.put("target",     escapeJson(edge.to()));
            e.put("confidence", edge.confidence());
            edgesArray.add(e);
        }
        data.set("links", edgesArray);

        String graphJson = mapper.writeValueAsString(data);

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Brain Graph</title>
              <style>
                body { margin:0; background:#1a1a2e; color:#eee; font-family:sans-serif; overflow:hidden; }
                canvas { display:block; }
                #tooltip {
                  position:absolute; display:none; background:rgba(0,0,0,.8);
                  color:#fff; padding:8px 12px; border-radius:6px; font-size:13px;
                  pointer-events:none; max-width:200px; word-wrap:break-word;
                }
                #info { position:absolute; top:10px; left:10px; font-size:12px; opacity:.6; }
              </style>
            </head>
            <body>
              <canvas id="c"></canvas>
              <div id="tooltip"></div>
              <div id="info">Brain Graph — %d nodes — drag to pan, scroll to zoom</div>
              <script>
            const GRAPH = %s;
            // Node type → colour
            const COLOURS = {
              CONCEPT:'#4fc3f7', ENTITY:'#aed581', DECISION:'#ffb74d',
              QUESTION:'#f48fb1', SOURCE:'#ce93d8', DEFAULT:'#90a4ae'
            };
            const canvas = document.getElementById('c');
            const ctx    = canvas.getContext('2d');
            const tip    = document.getElementById('tooltip');

            let W, H;
            function resize() {
              W = canvas.width  = window.innerWidth;
              H = canvas.height = window.innerHeight;
            }
            window.addEventListener('resize', resize);
            resize();

            // Initialise positions
            const nodes = GRAPH.nodes.map((n,i) => ({
              ...n,
              x: W/2 + (Math.random()-.5)*400,
              y: H/2 + (Math.random()-.5)*400,
              vx:0, vy:0, r:8
            }));
            const nodeMap = {};
            nodes.forEach(n => nodeMap[n.id] = n);
            const links = GRAPH.links
              .map(l => ({ source: nodeMap[l.source], target: nodeMap[l.target], w: l.confidence }))
              .filter(l => l.source && l.target);

            // Pan & zoom
            let px=0, py=0, scale=1, dragging=false, dragStart={x:0,y:0}, panStart={x:0,y:0};

            canvas.addEventListener('wheel', e => {
              e.preventDefault();
              const f = e.deltaY < 0 ? 1.1 : 0.9;
              scale = Math.max(.1, Math.min(5, scale*f));
            }, {passive:false});

            canvas.addEventListener('mousedown', e => {
              if (e.button !== 0) return;
              dragging = true;
              dragStart = {x:e.clientX, y:e.clientY};
              panStart  = {x:px, y:py};
            });
            window.addEventListener('mouseup',   () => dragging = false);
            window.addEventListener('mousemove', e => {
              if (dragging) { px = panStart.x + (e.clientX-dragStart.x); py = panStart.y + (e.clientY-dragStart.y); return; }
              // Hover tooltip
              const mx = (e.clientX - px - W/2) / scale, my = (e.clientY - py - H/2) / scale;
              let hit = null;
              for (const n of nodes) {
                const dx=n.x-mx, dy=n.y-my;
                if (dx*dx+dy*dy < n.r*n.r*4) { hit=n; break; }
              }
              if (hit) {
                tip.style.display='block';
                tip.style.left = (e.clientX+12)+'px';
                tip.style.top  = (e.clientY+12)+'px';
                tip.textContent = hit.label + ' [' + hit.type + ']';
              } else {
                tip.style.display='none';
              }
            });

            // Click: open wiki page
            canvas.addEventListener('click', e => {
              if (Math.abs(e.clientX-dragStart.x)+Math.abs(e.clientY-dragStart.y) > 5) return;
              const mx = (e.clientX - px - W/2) / scale, my = (e.clientY - py - H/2) / scale;
              for (const n of nodes) {
                const dx=n.x-mx, dy=n.y-my;
                if (dx*dx+dy*dy < n.r*n.r*4) {
                  window.open(encodeURIComponent(n.id) + '.md', '_blank'); return;
                }
              }
            });

            // Force simulation (Verlet / spring layout)
            const K = 0.005, REPEL = 3000, DAMP = 0.85;
            function simulate() {
              // Repulsion
              for (let i=0;i<nodes.length;i++) {
                for (let j=i+1;j<nodes.length;j++) {
                  const a=nodes[i], b=nodes[j];
                  let dx=b.x-a.x, dy=b.y-a.y;
                  const d2=dx*dx+dy*dy+0.01, f=REPEL/d2;
                  a.vx-=dx*f; a.vy-=dy*f; b.vx+=dx*f; b.vy+=dy*f;
                }
              }
              // Spring attraction
              for (const l of links) {
                const dx=l.target.x-l.source.x, dy=l.target.y-l.source.y;
                const d=Math.sqrt(dx*dx+dy*dy)+0.01;
                const f=K*(d-80);
                l.source.vx+=dx/d*f; l.source.vy+=dy/d*f;
                l.target.vx-=dx/d*f; l.target.vy-=dy/d*f;
              }
              // Gravity toward centre
              for (const n of nodes) {
                n.vx+=(0-n.x)*0.001; n.vy+=(0-n.y)*0.001;
                n.vx*=DAMP; n.vy*=DAMP;
                n.x+=n.vx; n.y+=n.vy;
              }
            }

            function draw() {
              ctx.clearRect(0,0,W,H);
              ctx.save();
              ctx.translate(W/2+px, H/2+py);
              ctx.scale(scale, scale);

              // Links
              ctx.globalAlpha=0.35;
              for (const l of links) {
                ctx.beginPath();
                ctx.moveTo(l.source.x, l.source.y);
                ctx.lineTo(l.target.x, l.target.y);
                ctx.strokeStyle='#aaa';
                ctx.lineWidth=1/scale;
                ctx.stroke();
              }
              ctx.globalAlpha=1;

              // Nodes
              for (const n of nodes) {
                ctx.beginPath();
                ctx.arc(n.x, n.y, n.r, 0, Math.PI*2);
                ctx.fillStyle = COLOURS[n.type] || COLOURS.DEFAULT;
                ctx.fill();
              }

              ctx.restore();
              simulate();
              requestAnimationFrame(draw);
            }
            draw();
              </script>
            </body>
            </html>
            """.formatted(graph.vertexSet().size(), graphJson);
    }

    /** Escapes a string for safe inclusion in JSON string values. */
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("&", "\\u0026");
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
