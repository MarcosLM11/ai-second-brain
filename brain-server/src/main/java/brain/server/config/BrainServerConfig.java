package brain.server.config;

import brain.core.port.WikiStore;
import brain.wiki.LogAppender;
import brain.wiki.WikiStoreFs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class BrainServerConfig {

    @Value("${brain.wiki-root:~/brain/wiki}")
    private String wikiRootRaw;

    @Bean
    public WikiStore wikiStore() {
        return new WikiStoreFs(expand(wikiRootRaw));
    }

    @Bean
    public LogAppender logAppender() {
        return new LogAppender(expand(wikiRootRaw));
    }

    public static Path expand(String raw) {
        if (raw.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), raw.substring(2));
        }
        return Path.of(raw);
    }
}
