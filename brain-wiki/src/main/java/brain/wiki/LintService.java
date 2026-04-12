package brain.wiki;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Structural lint checks on the wiki filesystem.
 *
 * <p>All operations are purely filesystem-based: no LLM calls, no SQLite.
 */
public class LintService {

    public record BrokenLink(String sourcePageId, String target) {}

    private static final Set<String> SYSTEM_PAGES = Set.of("index", "log");

    /**
     * Returns page IDs that have no incoming wikilinks from any other page.
     *
     * <p>index.md and log.md are never reported as orphans.
     * Self-referencing links ({@code [[page]]}) in a page do not count as incoming links
     * from "another" page.
     */
    public List<String> findOrphans(Path wikiRoot) {
        Path root = wikiRoot.toAbsolutePath().normalize();
        List<Path> mdFiles = listMdFiles(root);

        Set<String> referenced = new HashSet<>();
        for (Path file : mdFiles) {
            String pageId = toPageId(root, file);
            try {
                String content = Files.readString(file);
                for (String link : WikilinkExtractor.extract(content)) {
                    if (!link.equals(pageId)) {
                        referenced.add(link);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        List<String> orphans = new ArrayList<>();
        for (Path file : mdFiles) {
            String pageId = toPageId(root, file);
            if (SYSTEM_PAGES.contains(pageId)) continue;
            if (!referenced.contains(pageId)) {
                orphans.add(pageId);
            }
        }
        return List.copyOf(orphans);
    }

    /**
     * Returns all wikilinks whose target page does not exist on the filesystem.
     */
    public List<BrokenLink> findBrokenLinks(Path wikiRoot) {
        Path root = wikiRoot.toAbsolutePath().normalize();
        List<Path> mdFiles = listMdFiles(root);

        Set<String> existingIds = new HashSet<>();
        for (Path file : mdFiles) {
            existingIds.add(toPageId(root, file));
        }

        List<BrokenLink> broken = new ArrayList<>();
        for (Path file : mdFiles) {
            String pageId = toPageId(root, file);
            try {
                String content = Files.readString(file);
                for (String link : WikilinkExtractor.extract(content)) {
                    if (!existingIds.contains(link)) {
                        broken.add(new BrokenLink(pageId, link));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return List.copyOf(broken);
    }

    private List<Path> listMdFiles(Path root) {
        if (!Files.exists(root)) return List.of();
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".md"))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String toPageId(Path root, Path file) {
        String relative = root.relativize(file).toString();
        String withoutExt = relative.endsWith(".md") ? relative.substring(0, relative.length() - 3) : relative;
        return withoutExt.replace('\\', '/');
    }
}
