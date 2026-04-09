package brain.wiki;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class WikilinkExtractorTest {

    // --- simple links ---

    @Test
    void extractsSimpleLinks() throws Exception {
        String content = fixture("wikilinks/simple.md");

        List<String> links = WikilinkExtractor.extract(content);

        assertThat(links).contains("concept-a", "concept-b", "concept-c", "concept-d");
    }

    @Test
    void preservesDuplicates() throws Exception {
        String content = fixture("wikilinks/simple.md");

        List<String> links = WikilinkExtractor.extract(content);

        // concept-a appears twice in the fixture
        assertThat(links.stream().filter("concept-a"::equals).count()).isEqualTo(2);
    }

    // --- aliased links ---

    @Test
    void extractsPageTargetNotAlias() throws Exception {
        String content = fixture("wikilinks/with-aliases.md");

        List<String> links = WikilinkExtractor.extract(content);

        assertThat(links).contains("rsa-algorithm", "oauth2", "jwt-verification");
        assertThat(links).doesNotContain("RSA", "OAuth 2.0 Protocol");
    }

    // --- code blocks ---

    @Test
    void doesNotExtractLinksInFencedCodeBlocks() throws Exception {
        String content = fixture("wikilinks/code-blocks.md");

        List<String> links = WikilinkExtractor.extract(content);

        assertThat(links).contains("real-link", "after-code");
        assertThat(links).doesNotContain("ignored-fenced", "another-ignored");
    }

    @Test
    void doesNotExtractLinksInInlineCode() throws Exception {
        String content = fixture("wikilinks/code-blocks.md");

        List<String> links = WikilinkExtractor.extract(content);

        assertThat(links).doesNotContain("ignored-inline");
    }

    // --- edge cases ---

    @Test
    void returnsEmptyListForNullContent() {
        assertThat(WikilinkExtractor.extract(null)).isEmpty();
    }

    @Test
    void returnsEmptyListForBlankContent() {
        assertThat(WikilinkExtractor.extract("   ")).isEmpty();
    }

    @Test
    void returnsEmptyListWhenNoLinksPresent() {
        assertThat(WikilinkExtractor.extract("# Heading\n\nJust plain text.")).isEmpty();
    }

    @Test
    void handlesInlineLink() {
        List<String> links = WikilinkExtractor.extract("See [[my-concept]] for details.");

        assertThat(links).containsExactly("my-concept");
    }

    @Test
    void handlesLinkInHeading() {
        List<String> links = WikilinkExtractor.extract("# See [[concept-in-heading]]");

        assertThat(links).containsExactly("concept-in-heading");
    }

    // --- helpers ---

    private String fixture(String relativePath) throws IOException, URISyntaxException {
        var url = getClass().getClassLoader().getResource("fixtures/" + relativePath);
        assertThat(url).as("fixture not found: " + relativePath).isNotNull();
        return Files.readString(Path.of(url.toURI()));
    }
}
