package brain.wiki;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Append-only writer for {@code log.md}.
 *
 * <p>Each call to {@link #append} adds a level-2 heading with an ISO-8601 timestamp
 * followed by the entry text:
 * <pre>
 * ## [2026-04-09T10:00:00Z] Some entry
 * </pre>
 */
public class LogAppender {

    private static final String LOG_FILE = "log.md";

    private final Path logPath;

    public LogAppender(Path wikiRoot) {
        this.logPath = wikiRoot.toAbsolutePath().normalize().resolve(LOG_FILE);
    }

    public void append(String entry) {
        String line = "## [" + Instant.now() + "] " + entry + "\n\n";
        try {
            Files.createDirectories(logPath.getParent());
            if (!Files.exists(logPath)) {
                Files.writeString(logPath, "# Log\n\n");
            }
            Files.writeString(logPath, line, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
