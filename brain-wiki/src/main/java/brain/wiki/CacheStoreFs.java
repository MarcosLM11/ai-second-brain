package brain.wiki;

import brain.core.port.CacheStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.Optional;

/**
 * Filesystem-backed {@link CacheStore}.
 *
 * <p>Each cache entry is stored as {@code <cacheDir>/<sha256>.json}.
 * The directory is created on first write.
 */
public class CacheStoreFs implements CacheStore {

    private final Path cacheDir;

    public CacheStoreFs(Path cacheDir) {
        this.cacheDir = cacheDir.toAbsolutePath().normalize();
    }

    @Override
    public boolean isHit(String sha256) {
        return Files.exists(entryPath(sha256));
    }

    @Override
    public Optional<String> getLastProcessed(String sha256) {
        Path path = entryPath(sha256);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void set(String sha256, String metadata) {
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(entryPath(sha256), metadata);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path entryPath(String sha256) {
        return cacheDir.resolve(sha256 + ".json");
    }
}
