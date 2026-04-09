package brain.core.port;

import brain.core.model.WikiPage;

import java.util.List;
import java.util.Optional;

public interface WikiStore {

    Optional<WikiPage> read(String pageId);

    void write(String pageId, String content);

    List<WikiPage> list(String type, String pattern);

    List<WikiPage> listAll();

    Optional<String> readRaw(String relativePath);
}
