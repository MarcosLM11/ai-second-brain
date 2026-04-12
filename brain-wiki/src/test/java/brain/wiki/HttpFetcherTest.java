package brain.wiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class HttpFetcherTest {

    HttpFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new HttpFetcher();
    }

    // --- SSRF protection: private IPs ---

    @Test
    void rejectsLoopbackIpv4() {
        assertThatThrownBy(() -> fetcher.fetch("https://127.0.0.1/page"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SSRF blocked");
    }

    @Test
    void rejectsPrivateClassA() {
        assertThatThrownBy(() -> fetcher.fetch("https://10.0.0.1/resource"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SSRF blocked");
    }

    @Test
    void rejectsPrivateClassC() {
        assertThatThrownBy(() -> fetcher.fetch("https://192.168.1.100/admin"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SSRF blocked");
    }

    @Test
    void rejectsPrivateClassB() {
        assertThatThrownBy(() -> fetcher.fetch("https://172.16.0.1/internal"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SSRF blocked");
    }

    @Test
    void rejectsLinkLocal() {
        assertThatThrownBy(() -> fetcher.fetch("https://169.254.169.254/latest/meta-data/"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("SSRF blocked");
    }

    // --- Scheme validation ---

    @Test
    void rejectsHttpScheme() {
        assertThatThrownBy(() -> fetcher.fetch("http://example.com/article"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only HTTPS");
    }

    @Test
    void rejectsFtpScheme() {
        assertThatThrownBy(() -> fetcher.fetch("ftp://example.com/file.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only HTTPS");
    }

    @Test
    void rejectsFileScheme() {
        assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only HTTPS");
    }

    // --- Malformed URLs ---

    @Test
    void rejectsMalformedUrl() {
        assertThatThrownBy(() -> fetcher.fetch("not a url"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThatThrownBy(() -> fetcher.fetch("https:///path"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
