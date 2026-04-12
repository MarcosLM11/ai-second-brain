package brain.server.mcp;

import brain.core.port.CacheStore;
import brain.wiki.UrlFetcher;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MCP tools for the /ingest skill.
 *
 * <p>Provides: {@code fetch_url}, {@code fetch_file}, {@code source_hash},
 * {@code cache_check}, {@code cache_set}.
 */
@Component
public class IngestTools {

    private final UrlFetcher httpFetcher;
    private final CacheStore cacheStore;

    public IngestTools(UrlFetcher httpFetcher, CacheStore cacheStore) {
        this.httpFetcher = httpFetcher;
        this.cacheStore = cacheStore;
    }

    @Tool(description = """
        Fetch the body of an HTTPS URL. Only https:// is allowed.
        Private/loopback IPs are rejected (SSRF protection).
        Returns the response body as plain text (HTML, markdown, etc.).
        """)
    public String fetch_url(
        @ToolParam(description = "HTTPS URL to fetch") String url
    ) {
        try {
            return httpFetcher.fetch(url);
        } catch (IllegalArgumentException | SecurityException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR fetching URL: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: request interrupted";
        }
    }

    @Tool(description = """
        Read a local .md or .txt file and return its content.
        Accepts absolute paths or paths starting with ~/
        """)
    public String fetch_file(
        @ToolParam(description = "Absolute or ~/ path to a .md or .txt file") String path
    ) {
        Path resolved = expandPath(path);
        String filename = resolved.getFileName().toString();
        if (!filename.endsWith(".md") && !filename.endsWith(".txt")) {
            return "ERROR: Only .md and .txt files are supported. Got: " + filename;
        }
        if (!Files.exists(resolved)) {
            return "ERROR: File not found: " + resolved;
        }
        try {
            return Files.readString(resolved);
        } catch (IOException e) {
            return "ERROR reading file: " + e.getMessage();
        }
    }

    @Tool(description = """
        Compute the SHA-256 hex digest of the content of a source (file or URL).
        For local files: hashes the file content bytes.
        For URLs: downloads the response body and hashes it.
        Use this as the canonical key for cache_check and cache_set.
        """)
    public String source_hash(
        @ToolParam(description = "HTTPS URL or absolute/~/ path to a .md or .txt file") String source
    ) {
        try {
            byte[] content;
            if (source.startsWith("https://")) {
                content = httpFetcher.fetch(source).getBytes(StandardCharsets.UTF_8);
            } else {
                content = Files.readAllBytes(expandPath(source));
            }
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IllegalArgumentException | SecurityException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR reading source: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR: interrupted";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Tool(description = """
        Check whether a source has already been ingested.
        Returns "hit" (already ingested, skip) or "miss" (proceed with ingestion).
        """)
    public String cache_check(
        @ToolParam(description = "SHA-256 hex digest from source_hash") String sha256
    ) {
        return cacheStore.isHit(sha256) ? "hit" : "miss";
    }

    @Tool(description = """
        Record a successfully ingested source in the cache.
        Call this after all wiki pages for the source have been written.
        """)
    public String cache_set(
        @ToolParam(description = "SHA-256 hex digest from source_hash") String sha256,
        @ToolParam(description = "JSON metadata string, e.g. {\"url\":\"...\",\"pages\":[...],\"date\":\"...\"}") String metadata
    ) {
        cacheStore.set(sha256, metadata);
        return "Cached: " + sha256;
    }

    private static Path expandPath(String raw) {
        if (raw.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), raw.substring(2));
        }
        return Path.of(raw);
    }
}
