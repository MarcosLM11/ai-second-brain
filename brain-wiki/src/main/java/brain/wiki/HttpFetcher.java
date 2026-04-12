package brain.wiki;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;

/**
 * HTTPS-only URL fetcher with SSRF protection (RNF-SEC-01).
 *
 * <p>Blocks any URL that:
 * <ul>
 *   <li>uses a scheme other than {@code https}</li>
 *   <li>resolves to a loopback, site-local, link-local, or any-local address</li>
 * </ul>
 */
public class HttpFetcher implements UrlFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient http;

    public HttpFetcher() {
        this.http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    /**
     * Fetches the body of a URL.
     *
     * @param url the URL to fetch (must be https://)
     * @return response body as a String
     * @throws IllegalArgumentException if the URL is malformed or uses non-https scheme
     * @throws SecurityException if the URL resolves to a private/reserved IP (SSRF)
     * @throws IOException on HTTP or I/O error
     * @throws InterruptedException if the request is interrupted
     */
    public String fetch(String url) throws IOException, InterruptedException {
        URI uri = parseAndValidate(url);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(TIMEOUT)
            .GET()
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return response.body();
    }

    private URI parseAndValidate(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                "Only HTTPS URLs are allowed. Received scheme: " + uri.getScheme());
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL has no host: " + url);
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                rejectPrivateAddress(addr, url);
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host '" + host + "': " + e.getMessage(), e);
        }

        return uri;
    }

    private void rejectPrivateAddress(InetAddress addr, String url) {
        if (addr.isLoopbackAddress()) {
            throw new SecurityException("SSRF blocked: loopback address resolved for URL: " + url);
        }
        if (addr.isSiteLocalAddress()) {
            throw new SecurityException("SSRF blocked: private (RFC-1918) address resolved for URL: " + url);
        }
        if (addr.isLinkLocalAddress()) {
            throw new SecurityException("SSRF blocked: link-local address resolved for URL: " + url);
        }
        if (addr.isAnyLocalAddress()) {
            throw new SecurityException("SSRF blocked: any-local address resolved for URL: " + url);
        }
        if (addr.isMulticastAddress()) {
            throw new SecurityException("SSRF blocked: multicast address resolved for URL: " + url);
        }
    }
}
