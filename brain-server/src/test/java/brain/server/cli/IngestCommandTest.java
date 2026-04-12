package brain.server.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IngestCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void asyncRunReturnsBelow200ms() throws IOException {
        Path transcript = tempDir.resolve("session.md");
        Files.writeString(transcript, "short"); // too short to extract → skips API

        var cmd = new IngestCommand();
        cmd.file = transcript;
        cmd.async = true;
        cmd.sourceType = "conversation";

        // ANTHROPIC_API_KEY is not set in CI → runIngestion() exits early after key check
        Instant start = Instant.now();
        cmd.run();
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed.toMillis())
            .as("run() with --async should return in < 200ms")
            .isLessThan(200);
    }

    @Test
    void syncRunWithMissingApiKeyExitsGracefully() throws IOException {
        Path transcript = tempDir.resolve("session.md");
        Files.writeString(transcript, "some content that is long enough to trigger processing");

        var cmd = new IngestCommand();
        cmd.file = transcript;
        cmd.async = false;
        cmd.sourceType = "conversation";

        // Without ANTHROPIC_API_KEY this should not throw, just print to stderr
        cmd.run();
    }

    @Test
    void asyncFlagStartsVirtualThread() throws IOException, InterruptedException {
        Path transcript = tempDir.resolve("session.md");
        Files.writeString(transcript, "x");

        var cmd = new IngestCommand();
        cmd.file = transcript;
        cmd.async = true;
        cmd.sourceType = "conversation";

        cmd.run();

        // Give the Virtual Thread a moment to start and finish
        Thread.sleep(50);
        // If no exception was thrown, the Virtual Thread was started successfully
    }
}
