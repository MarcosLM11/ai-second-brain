package brain.wiki;

import java.io.IOException;

@FunctionalInterface
public interface UrlFetcher {
    String fetch(String url) throws IOException, InterruptedException;
}
