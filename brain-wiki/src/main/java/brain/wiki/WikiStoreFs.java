package brain.wiki;

import brain.core.model.NodeType;
import brain.core.model.WikiPage;
import brain.core.port.WikiStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Filesystem implementation of {@link WikiStore}.
 *
 * <p>Pages are stored as {@code <wikiRoot>/<pageId>.md}. The {@code pageId} is the
 * relative path without the {@code .md} extension (e.g. {@code concepts/jwt-verification}).
 *
 * <p>Path traversal is rejected per RNF-SEC-01: any resolved path that escapes
 * {@code wikiRoot} throws {@link SecurityException}.
 */
public class WikiStoreFs implements WikiStore {

    private final Path wikiRoot;
    private final FrontmatterParser parser;

    public WikiStoreFs(Path wikiRoot) {
        this.wikiRoot = wikiRoot.toAbsolutePath().normalize();
        this.parser = new FrontmatterParser();
    }

    @Override
    public Optional<WikiPage> read(String pageId) {
        Path target = resolvePagePath(pageId);
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(target);
            return Optional.of(toWikiPage(pageId, target, content));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void write(String pageId, String content) {
        Path target = resolvePagePath(pageId);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<WikiPage> list(String type, String pattern) {
        return listAll().stream()
            .filter(p -> type == null || type.isBlank() || p.type().name().equalsIgnoreCase(type))
            .filter(p -> pattern == null || pattern.isBlank() || matchesGlob(p.id(), pattern))
            .toList();
    }

    @Override
    public List<WikiPage> listAll() {
        if (!Files.exists(wikiRoot)) {
            return List.of();
        }
        var pages = new ArrayList<WikiPage>();
        try {
            Files.walkFileTree(wikiRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!file.getFileName().toString().endsWith(".md")) {
                        return FileVisitResult.CONTINUE;
                    }
                    String pageId = toPageId(file);
                    if (pageId.equals("index") || pageId.equals("log")) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        String content = Files.readString(file);
                        pages.add(toWikiPage(pageId, file, content));
                    } catch (IOException ignored) {
                        // skip unreadable files
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return List.copyOf(pages);
    }

    @Override
    public Optional<String> readRaw(String relativePath) {
        Path target = wikiRoot.resolve(relativePath).normalize();
        guardPath(target);
        if (!Files.exists(target)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(target));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // --- private helpers ---

    private Path resolvePagePath(String pageId) {
        Path target = wikiRoot.resolve(pageId + ".md").normalize();
        guardPath(target);
        return target;
    }

    /** RNF-SEC-01: reject any path that escapes wikiRoot. */
    private void guardPath(Path target) {
        if (!target.startsWith(wikiRoot)) {
            throw new SecurityException("Path traversal attempt detected: " + target);
        }
    }

    private String toPageId(Path absoluteFile) {
        String relative = wikiRoot.relativize(absoluteFile).toString();
        String withoutExt = relative.endsWith(".md") ? relative.substring(0, relative.length() - 3) : relative;
        return withoutExt.replace('\\', '/');
    }

    private boolean matchesGlob(String pageId, String pattern) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        return matcher.matches(Path.of(pageId));
    }

    private WikiPage toWikiPage(String pageId, Path path, String content) {
        Map<String, Object> fm = parser.parse(content);

        NodeType type = parseType(fm.getOrDefault("type", "").toString());
        String title = fm.getOrDefault("title", pageId).toString();
        List<String> aliases = parseStringList(fm.get("aliases"));
        List<String> tags = parseStringList(fm.get("tags"));
        List<String> sources = parseStringList(fm.get("sources"));
        Instant created = parseDate(fm.get("created"));
        Instant updated = parseDate(fm.get("updated"));

        return new WikiPage(pageId, path, type, title, aliases, tags, sources, created, updated, content);
    }

    private NodeType parseType(String value) {
        return switch (value.toLowerCase()) {
            case "entity"   -> NodeType.ENTITY;
            case "decision" -> NodeType.DECISION;
            case "question" -> NodeType.QUESTION;
            case "source"   -> NodeType.SOURCE;
            default         -> NodeType.CONCEPT;
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    private Instant parseDate(Object value) {
        if (value == null) return Instant.now();
        try {
            return LocalDate.parse(value.toString()).atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
