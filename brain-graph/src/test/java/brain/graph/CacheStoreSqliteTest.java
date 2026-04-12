package brain.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CacheStoreSqliteTest {

    @TempDir
    Path tempDir;

    CacheStoreSqlite store;

    @BeforeEach
    void setUp() {
        store = new CacheStoreSqlite(tempDir.resolve("cache.db"));
    }

    @Test
    void unknownHashIsNotHit() {
        assertThat(store.isHit("abc123")).isFalse();
    }

    @Test
    void unknownHashHasNoLastProcessed() {
        assertThat(store.getLastProcessed("abc123")).isEmpty();
    }

    @Test
    void afterSetHashIsHit() {
        store.set("deadbeef", "{}");

        assertThat(store.isHit("deadbeef")).isTrue();
    }

    @Test
    void afterSetLastProcessedIsPresent() {
        store.set("deadbeef", "{}");

        assertThat(store.getLastProcessed("deadbeef")).isPresent();
    }

    @Test
    void lastProcessedIsIso8601() {
        store.set("deadbeef", "{}");

        String lastProcessed = store.getLastProcessed("deadbeef").orElseThrow();
        assertThat(lastProcessed).matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
    }

    @Test
    void roundTripPersistReloadCheck() {
        String hash = "a".repeat(64);
        store.set(hash, "{}");

        // Reload from same DB file
        CacheStoreSqlite reloaded = new CacheStoreSqlite(tempDir.resolve("cache.db"));
        assertThat(reloaded.isHit(hash)).isTrue();
        assertThat(reloaded.getLastProcessed(hash)).isPresent();
    }

    @Test
    void setIsIdempotent() {
        store.set("deadbeef", "{}");
        store.set("deadbeef", "{}");

        assertThat(store.isHit("deadbeef")).isTrue();
    }
}
