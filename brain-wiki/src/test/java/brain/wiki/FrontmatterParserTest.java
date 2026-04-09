package brain.wiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class FrontmatterParserTest {

    private FrontmatterParser parser;

    @BeforeEach
    void setUp() {
        parser = new FrontmatterParser();
    }

    // --- valid frontmatter ---

    @Test
    void parsesValidFrontmatter() throws Exception {
        String content = fixture("frontmatter/valid.md");

        Map<String, Object> fm = parser.parse(content);

        assertThat(fm).containsEntry("title", "JWT Verification");
        assertThat(fm).containsEntry("type", "concept");
        assertThat(fm).extractingByKey("aliases").asList().containsExactly("JWT", "JSON Web Token");
        assertThat(fm).extractingByKey("tags").asList().containsExactly("security", "authentication");
        assertThat(fm).extractingByKey("sources").asList().containsExactly("sources/rfc7519");
        assertThat(fm).containsKey("created");
        assertThat(fm).containsKey("updated");
    }

    @Test
    void bodyExcludesFrontmatter() throws Exception {
        String content = fixture("frontmatter/valid.md");

        String body = parser.body(content);

        assertThat(body).contains("# JWT Verification");
        assertThat(body).doesNotContain("title:");
        assertThat(body).doesNotContain("type:");
    }

    // --- empty frontmatter ---

    @Test
    void returnsEmptyMapForEmptyFrontmatter() throws Exception {
        String content = fixture("frontmatter/empty.md");

        Map<String, Object> fm = parser.parse(content);

        assertThat(fm).isEmpty();
    }

    @Test
    void bodyIsReturnedWhenFrontmatterIsEmpty() throws Exception {
        String content = fixture("frontmatter/empty.md");

        String body = parser.body(content);

        assertThat(body).contains("# Empty Frontmatter");
    }

    // --- absent frontmatter ---

    @Test
    void returnsEmptyMapWhenFrontmatterAbsent() throws Exception {
        String content = fixture("frontmatter/absent.md");

        Map<String, Object> fm = parser.parse(content);

        assertThat(fm).isEmpty();
    }

    @Test
    void bodyIsFullContentWhenFrontmatterAbsent() throws Exception {
        String content = fixture("frontmatter/absent.md");

        String body = parser.body(content);

        assertThat(body).isEqualTo(content);
    }

    // --- invalid YAML ---

    @Test
    void returnsEmptyMapForInvalidYaml() throws Exception {
        String content = fixture("frontmatter/invalid-yaml.md");

        Map<String, Object> fm = parser.parse(content);

        assertThat(fm).isEmpty();
    }

    // --- incomplete frontmatter ---

    @Test
    void parsesIncompleteFrontmatter() throws Exception {
        String content = fixture("frontmatter/incomplete.md");

        Map<String, Object> fm = parser.parse(content);

        assertThat(fm).containsEntry("title", "Partial Entry");
        assertThat(fm).containsEntry("type", "decision");
        assertThat(fm).doesNotContainKey("aliases");
        assertThat(fm).doesNotContainKey("tags");
    }

    // --- edge cases ---

    @Test
    void returnsEmptyMapForNullContent() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void returnsEmptyMapForEmptyString() {
        assertThat(parser.parse("")).isEmpty();
    }

    @Test
    void returnsEmptyMapWhenClosingDelimiterMissing() {
        String content = "---\ntitle: Orphan\n";

        assertThat(parser.parse(content)).isEmpty();
    }

    @Test
    void doesNotConfuseSeTextHeadingWithFrontmatter() {
        // A document that starts with content, not ---
        String content = "Some text\n---\nMore text";

        assertThat(parser.parse(content)).isEmpty();
    }

    // --- helpers ---

    private String fixture(String relativePath) throws IOException, URISyntaxException {
        var url = getClass().getClassLoader().getResource("fixtures/" + relativePath);
        assertThat(url).as("fixture not found: " + relativePath).isNotNull();
        return Files.readString(Path.of(url.toURI()));
    }
}
