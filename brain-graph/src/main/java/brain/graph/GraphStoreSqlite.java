package brain.graph;

import brain.core.model.EdgeOrigin;
import brain.core.model.EdgeType;
import brain.core.model.GraphEdge;
import brain.core.model.GraphNode;
import brain.core.model.NodeType;
import brain.core.port.GraphStore;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GraphStoreSqlite implements GraphStore {

    private final String url;

    public GraphStoreSqlite(Path dbPath) {
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initSchema();
    }

    private void initSchema() {
        try (var conn = connect(); var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS nodes (
                    id          TEXT PRIMARY KEY,
                    label       TEXT NOT NULL,
                    type        TEXT NOT NULL,
                    community   INTEGER DEFAULT -1,
                    created_at  INTEGER NOT NULL,
                    updated_at  INTEGER NOT NULL
                )
                """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edges (
                    from_id     TEXT NOT NULL,
                    to_id       TEXT NOT NULL,
                    type        TEXT NOT NULL,
                    origin      TEXT NOT NULL,
                    confidence  REAL NOT NULL,
                    timestamp   INTEGER NOT NULL,
                    PRIMARY KEY (from_id, to_id, type)
                )
                """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS page_cache (
                    page_id      TEXT PRIMARY KEY,
                    sha256       TEXT NOT NULL,
                    processed_at INTEGER NOT NULL
                )
                """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite schema", e);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    @Override
    public void persist(Graph<String, DefaultWeightedEdge> graph, List<GraphEdge> edges) {
        try (var conn = connect()) {
            conn.setAutoCommit(false);

            try (var ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO nodes (id, label, type, community, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
                long now = Instant.now().toEpochMilli();
                for (String vertex : graph.vertexSet()) {
                    ps.setString(1, vertex);
                    ps.setString(2, vertex);
                    ps.setString(3, NodeType.CONCEPT.name());
                    ps.setInt(4, -1);
                    ps.setLong(5, now);
                    ps.setLong(6, now);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (var ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO edges (from_id, to_id, type, origin, confidence, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
                for (GraphEdge edge : edges) {
                    ps.setString(1, edge.from());
                    ps.setString(2, edge.to());
                    ps.setString(3, edge.type().name());
                    ps.setString(4, edge.origin().name());
                    ps.setDouble(5, edge.confidence());
                    ps.setLong(6, edge.timestamp().toEpochMilli());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to persist graph", e);
        }
    }

    @Override
    public Graph<String, DefaultWeightedEdge> load() {
        var graph = new DefaultDirectedWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);

        try (var conn = connect()) {
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT id FROM nodes")) {
                while (rs.next()) {
                    graph.addVertex(rs.getString("id"));
                }
            }

            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery("SELECT from_id, to_id, confidence FROM edges")) {
                while (rs.next()) {
                    String from = rs.getString("from_id");
                    String to = rs.getString("to_id");
                    double confidence = rs.getDouble("confidence");
                    graph.addVertex(from);
                    graph.addVertex(to);
                    var e = graph.addEdge(from, to);
                    if (e != null) {
                        graph.setEdgeWeight(e, confidence);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load graph", e);
        }

        return graph;
    }

    @Override
    public Optional<GraphNode> getNode(String id) {
        try (var conn = connect();
             var ps = conn.prepareStatement(
                 "SELECT id, label, type, community, created_at, updated_at FROM nodes WHERE id = ?")) {
            ps.setString(1, id);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new GraphNode(
                        rs.getString("id"),
                        rs.getString("label"),
                        NodeType.valueOf(rs.getString("type")),
                        rs.getInt("community"),
                        Instant.ofEpochMilli(rs.getLong("created_at")),
                        Instant.ofEpochMilli(rs.getLong("updated_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get node: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public void saveNode(GraphNode node) {
        try (var conn = connect();
             var ps = conn.prepareStatement("""
                 INSERT OR REPLACE INTO nodes (id, label, type, community, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?)
                 """)) {
            ps.setString(1, node.id());
            ps.setString(2, node.label());
            ps.setString(3, node.type().name());
            ps.setInt(4, node.community());
            ps.setLong(5, node.createdAt().toEpochMilli());
            ps.setLong(6, node.updatedAt().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save node: " + node.id(), e);
        }
    }

    public boolean isPageCached(String pageId, String sha256) {
        try (var conn = connect();
             var ps = conn.prepareStatement(
                 "SELECT 1 FROM page_cache WHERE page_id = ? AND sha256 = ?")) {
            ps.setString(1, pageId);
            ps.setString(2, sha256);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check page cache: " + pageId, e);
        }
    }

    public void updatePageCache(String pageId, String sha256) {
        try (var conn = connect();
             var ps = conn.prepareStatement("""
                 INSERT OR REPLACE INTO page_cache (page_id, sha256, processed_at)
                 VALUES (?, ?, ?)
                 """)) {
            ps.setString(1, pageId);
            ps.setString(2, sha256);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update page cache: " + pageId, e);
        }
    }

    public void deleteEdgesFrom(String pageId) {
        try (var conn = connect();
             var ps = conn.prepareStatement("DELETE FROM edges WHERE from_id = ?")) {
            ps.setString(1, pageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete edges from: " + pageId, e);
        }
    }

    public void insertEdges(List<GraphEdge> edges) {
        if (edges.isEmpty()) return;
        try (var conn = connect()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement("""
                INSERT OR REPLACE INTO edges (from_id, to_id, type, origin, confidence, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
                for (GraphEdge edge : edges) {
                    ps.setString(1, edge.from());
                    ps.setString(2, edge.to());
                    ps.setString(3, edge.type().name());
                    ps.setString(4, edge.origin().name());
                    ps.setDouble(5, edge.confidence());
                    ps.setLong(6, edge.timestamp().toEpochMilli());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert edges", e);
        }
    }

    public void insertNodeIfAbsent(GraphNode node) {
        try (var conn = connect();
             var ps = conn.prepareStatement("""
                 INSERT OR IGNORE INTO nodes (id, label, type, community, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?)
                 """)) {
            ps.setString(1, node.id());
            ps.setString(2, node.label());
            ps.setString(3, node.type().name());
            ps.setInt(4, node.community());
            ps.setLong(5, node.createdAt().toEpochMilli());
            ps.setLong(6, node.updatedAt().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert node if absent: " + node.id(), e);
        }
    }

    public List<GraphEdge> loadEdges() {
        var result = new ArrayList<GraphEdge>();
        try (var conn = connect();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(
                 "SELECT from_id, to_id, type, origin, confidence, timestamp FROM edges")) {
            while (rs.next()) {
                result.add(new GraphEdge(
                    rs.getString("from_id"),
                    rs.getString("to_id"),
                    EdgeType.valueOf(rs.getString("type")),
                    EdgeOrigin.valueOf(rs.getString("origin")),
                    rs.getDouble("confidence"),
                    Instant.ofEpochMilli(rs.getLong("timestamp"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load edges", e);
        }
        return result;
    }
}
