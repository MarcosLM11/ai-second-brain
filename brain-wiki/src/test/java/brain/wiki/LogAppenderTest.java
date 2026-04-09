package brain.wiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogAppenderTest {

    @TempDir
    Path wikiRoot;

    LogAppender appender;

    @BeforeEach
    void setUp() {
        appender = new LogAppender(wikiRoot);
    }

    @Test
    void appendCreatesLogFileWhenMissing() throws IOException {
        appender.append("Session started");

        assertThat(wikiRoot.resolve("log.md")).exists();
    }

    @Test
    void appendWritesEntryWithIsoTimestamp() throws IOException {
        appender.append("Session started");

        String content = Files.readString(wikiRoot.resolve("log.md"));
        assertThat(content).containsPattern("## \\[\\d{4}-\\d{2}-\\d{2}T.*Z\\] Session started");
    }

    @Test
    void appendDoesNotOverwriteExistingEntries() throws IOException {
        appender.append("First entry");
        appender.append("Second entry");

        String content = Files.readString(wikiRoot.resolve("log.md"));
        assertThat(content).contains("First entry");
        assertThat(content).contains("Second entry");
    }

    @Test
    void appendAddsEntriesInOrder() throws IOException {
        appender.append("Alpha");
        appender.append("Beta");

        String content = Files.readString(wikiRoot.resolve("log.md"));
        assertThat(content.indexOf("Alpha")).isLessThan(content.indexOf("Beta"));
    }

    @Test
    void appendPreservesExistingLogHeader() throws IOException {
        appender.append("First");
        appender.append("Second");

        String content = Files.readString(wikiRoot.resolve("log.md"));
        assertThat(content).startsWith("# Log\n");
    }
}
