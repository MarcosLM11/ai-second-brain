package brain.core.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record WikiPage(
    String       id,
    Path         path,
    NodeType     type,
    String       title,
    List<String> aliases,
    List<String> tags,
    List<String> sources,
    Instant      created,
    Instant      updated,
    String       content
) {}
