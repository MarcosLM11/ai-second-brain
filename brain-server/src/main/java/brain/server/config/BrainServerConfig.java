package brain.server.config;

import brain.core.port.CacheStore;
import brain.core.port.WikiStore;
import brain.graph.CacheStoreSqlite;
import brain.graph.GraphAnalyzer;
import brain.graph.GraphBuilder;
import brain.graph.GraphReportWriter;
import brain.graph.GraphStoreSqlite;
import brain.graph.GraphTraversal;
import brain.search.SearchIndexer;
import brain.wiki.HttpFetcher;
import brain.wiki.LintReportWriter;
import brain.wiki.LintService;
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
        return new CacheStoreSqlite(expand(graphDbRaw));
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

    @Bean
    public GraphAnalyzer graphAnalyzer() {
        return new GraphAnalyzer();
    }

    @Bean
    public GraphReportWriter graphReportWriter() {
        return new GraphReportWriter(expand(wikiRootRaw));
    }

    @Bean
    public LintService lintService() {
        return new LintService();
    }

    @Bean
    public LintReportWriter lintReportWriter() {
        return new LintReportWriter(expand(wikiRootRaw));
    }

    @Bean
    public SearchIndexer searchIndexer() {
        return new SearchIndexer(expand(graphDbRaw));
    }

    public static Path expand(String raw) {
        if (raw.startsWith("~/")) {
            return Path.of(System.getProperty("user.home"), raw.substring(2));
        }
        return Path.of(raw);
    }
}
