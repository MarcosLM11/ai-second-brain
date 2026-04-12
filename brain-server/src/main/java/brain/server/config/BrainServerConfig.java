package brain.server.config;

import brain.core.port.CacheStore;
import brain.core.port.WikiStore;
import brain.graph.GraphBuilder;
import brain.graph.GraphStoreSqlite;
import brain.graph.GraphTraversal;
import brain.wiki.CacheStoreFs;
import brain.wiki.HttpFetcher;
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

    @Value("${brain.cache-dir:~/brain/cache}")
    private String cacheDirRaw;

    @Value("${brain.graph-db:~/brain/brain_graph.db}")
    private String graphDbRaw;

    @Bean
    public WikiStore wikiStore() {
        return new WikiStoreFs(expand(wikiRootRaw));
    }

    @Bean
    public LogAppender logAppender() {
        return new LogAppender(expand(wikiRootRaw));
    }

    @Bean
    public HttpFetcher httpFetcher() {
        return new HttpFetcher();
    }

    @Bean
    public CacheStore cacheStore() {
        return new CacheStoreFs(expand(cacheDirRaw));
    }

    @Bean
    public GraphStoreSqlite graphStoreSqlite() {
        return new GraphStoreSqlite(expand(graphDbRaw));
    }

    @Bean
    public GraphBuilder graphBuilder(WikiStore wikiStore, GraphStoreSqlite graphStoreSqlite) {
        return new GraphBuilder(wikiStore, graphStoreSqlite);
    }

    @Bean
    public GraphTraversal graphTraversal(GraphStoreSqlite graphStoreSqlite) {
        return new GraphTraversal(graphStoreSqlite);
    }

    public static Path expand(String raw) {
        if (raw.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), raw.substring(2));
        }
        return Path.of(raw);
    }
}
