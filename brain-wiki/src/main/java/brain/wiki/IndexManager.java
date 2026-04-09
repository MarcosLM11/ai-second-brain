package brain.wiki;

import brain.core.model.WikiPage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintains {@code index.md} as a markdown table cataloguing every wiki page
 * with its id, title, type and a short summary extracted from the page body.
 */
public class IndexManager {

    private static final String INDEX_FILE = "index.md";

    private final Path indexPath;

    public IndexManager(Path wikiRoot) {
        this.indexPath = wikiRoot.toAbsolutePath().normalize().resolve(INDEX_FILE);
    }

    public void addOrUpdate(WikiPage page) {
        Map<String, String[]> entries = readEntries();
        entries.put(page.id(), rowValues(page));
        writeEntries(entries);
    }

    public void remove(String pageId) {
        Map<String, String[]> entries = readEntries();
        entries.remove(pageId);
        writeEntries(entries);
    }

    public void rebuild(List<WikiPage> pages) {
        Map<String, String[]> entries = new LinkedHashMap<>();
        for (WikiPage page : pages) {
            entries.put(page.id(), rowValues(page));
        }
        writeEntries(entries);
    }

    // --- private helpers ---

    private String[] rowValues(WikiPage page) {
        return new String[]{page.title(), page.type().name(), extractSummary(page.content())};
    }

    private String extractSummary(String content) {
        List<String> lines = content.lines().toList();
        int bodyStart = 0;
        if (!lines.isEmpty() && lines.get(0).trim().equals("---")) {
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("---")) {
                    bodyStart = i + 1;
                    break;
                }
            }
        }
        for (int i = bodyStart; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                return trimmed.length() > 80 ? trimmed.substring(0, 77) + "..." : trimmed;
            }
        }
        return "";
    }

    private Map<String, String[]> readEntries() {
        if (!Files.exists(indexPath)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String[]> entries = new LinkedHashMap<>();
            for (String line : Files.readAllLines(indexPath)) {
                if (!line.startsWith("| ") || line.startsWith("| page") || line.startsWith("|---")) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String pageId = parts[1].trim();
                if (pageId.isBlank()) continue;
                entries.put(pageId, new String[]{
                    unescape(parts[2].trim()),
                    unescape(parts[3].trim()),
                    unescape(parts[4].trim())
                });
            }
            return entries;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeEntries(Map<String, String[]> entries) {
        var sb = new StringBuilder("# Index\n\n");
        sb.append("| page | title | type | summary |\n");
        sb.append("|------|-------|------|---------|\n");
        for (Map.Entry<String, String[]> e : entries.entrySet()) {
            String[] v = e.getValue();
            sb.append("| ").append(escape(e.getKey()))
              .append(" | ").append(escape(v[0]))
              .append(" | ").append(escape(v[1]))
              .append(" | ").append(escape(v[2]))
              .append(" |\n");
        }
        try {
            Files.createDirectories(indexPath.getParent());
            Files.writeString(indexPath, sb.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String escape(String value) {
        return value.replace("|", "\\|");
    }

    private String unescape(String value) {
        return value.replace("\\|", "|");
    }
}
