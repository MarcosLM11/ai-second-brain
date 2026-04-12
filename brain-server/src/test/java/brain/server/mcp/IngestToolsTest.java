package brain.server.mcp;

import brain.core.port.CacheStore;
import brain.wiki.UrlFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestToolsTest {

    @TempDir
    Path tempDir;

    @Mock UrlFetcher urlFetcher;
    @Mock CacheStore cacheStore;

    IngestTools tools;

    @BeforeEach
    void setUp() {
        tools = new IngestTools(urlFetcher, cacheStore);
    }

    // --- source_hash: local file ---

    @Test
    void sourceHashFileIsReproducible() throws IOException {
        Path file = tempDir.resolve("note.md");
        Files.writeString(file, "hello world");

        String hash1 = tools.source_hash(file.toString());
        String hash2 = tools.source_hash(file.toString());

        assertThat(hash1).hasSize(64);
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sourceHashFileChangesWhenContentChanges() throws IOException {
        Path file = tempDir.resolve("note.md");
        Files.writeString(file, "version one");
        String hash1 = tools.source_hash(file.toString());

        Files.writeString(file, "version two");
        String hash2 = tools.source_hash(file.toString());

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void sourceHashFileMatchesExpectedSha256() throws Exception {
        Path file = tempDir.resolve("exact.md");
        byte[] bytes = "deterministic".getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);

        String expected = sha256Hex(bytes);
        assertThat(tools.source_hash(file.toString())).isEqualTo(expected);
    }

    // --- source_hash: URL (mocked, no network) ---

    @Test
    void sourceHashUrlReturnsHash() throws Exception {
        when(urlFetcher.fetch("https://example.com/article")).thenReturn("article content");

        String hash = tools.source_hash("https://example.com/article");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void sourceHashUrlMatchesExpectedSha256() throws Exception {
        String body = "fetched body";
        when(urlFetcher.fetch("https://example.com/page")).thenReturn(body);

        String expected = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        assertThat(tools.source_hash("https://example.com/page")).isEqualTo(expected);
    }

    @Test
    void sourceHashUrlReturnsErrorOnFetchFailure() throws Exception {
        when(urlFetcher.fetch("https://example.com/fail"))
            .thenThrow(new IOException("connection refused"));

        String result = tools.source_hash("https://example.com/fail");

        assertThat(result).startsWith("ERROR");
    }

    // --- cache_check ---

    @Test
    void cacheCheckReturnsMissJsonWhenNotCached() {
        when(cacheStore.isHit("abc123")).thenReturn(false);

        assertThat(tools.cache_check("abc123")).isEqualTo("{\"hit\":false}");
    }

    @Test
    void cacheCheckReturnsHitJsonWithLastProcessed() {
        when(cacheStore.isHit("abc123")).thenReturn(true);
        when(cacheStore.getLastProcessed("abc123")).thenReturn(java.util.Optional.of("2026-04-12T10:00:00Z"));

        String result = tools.cache_check("abc123");

        assertThat(result).contains("\"hit\":true");
        assertThat(result).contains("\"lastProcessed\":\"2026-04-12T10:00:00Z\"");
    }

    // --- helper ---

    private static String sha256Hex(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
