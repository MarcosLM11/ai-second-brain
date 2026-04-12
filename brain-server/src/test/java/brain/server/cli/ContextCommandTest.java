package brain.server.cli;

import brain.graph.GraphStoreSqlite;
import brain.graph.GraphTraversal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class ContextCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void emptyGraphProducesNoOutput() {
        var store = new GraphStoreSqlite(tempDir.resolve("graph.db"));
        var traversal = new GraphTraversal(store);

        var out = new ByteArrayOutputStream();
        var original = System.out;
        System.setOut(new PrintStream(out));
        try {
            String context = traversal.buildSessionContext("nonexistent-node", 2000);
            if (!context.isBlank()) {
                System.out.print(context);
            }
        } finally {
            System.setOut(original);
        }

        assertThat(out.toString()).isEmpty();
    }

    @Test
    void missingNodeProducesNoOutput() {
        var store = new GraphStoreSqlite(tempDir.resolve("graph2.db"));
        var traversal = new GraphTraversal(store);

        var out = new ByteArrayOutputStream();
        var original = System.out;
        System.setOut(new PrintStream(out));
        try {
            String context = traversal.buildSessionContext("some-project-that-does-not-exist", 2000);
            if (!context.isBlank()) {
                System.out.print(context);
            }
        } finally {
            System.setOut(original);
        }

        assertThat(out.toString()).isEmpty();
    }
}
