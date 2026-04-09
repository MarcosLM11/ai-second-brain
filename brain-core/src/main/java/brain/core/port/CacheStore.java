package brain.core.port;

import java.util.Optional;

public interface CacheStore {

    boolean isHit(String sha256);

    Optional<String> getLastProcessed(String sha256);

    void set(String sha256, String metadata);
}
