package brain.wiki;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Map;

/**
 * Extracts and parses the YAML frontmatter block (between {@code ---} delimiters)
 * from a markdown document.
 *
 * <p>Returns a {@code Map<String, Object>} so that callers can interpret specific
 * fields without coupling this parser to the domain model.
 */
public class FrontmatterParser {

    private static final String DELIMITER = "---";
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /**
     * Parses the frontmatter of {@code content}.
     *
     * @return the deserialized YAML fields, or an empty map when frontmatter is
     *         absent, empty, or contains invalid YAML.
     */
    public Map<String, Object> parse(String content) {
        if (content == null || !content.startsWith(DELIMITER)) {
            return Map.of();
        }

        int yamlStart = DELIMITER.length();
        // The opening --- must be followed by a newline (or end of string)
        if (yamlStart < content.length() && content.charAt(yamlStart) != '\n' && content.charAt(yamlStart) != '\r') {
            return Map.of();
        }

        int closingPos = content.indexOf('\n' + DELIMITER, yamlStart);
        if (closingPos == -1) {
            return Map.of();
        }

        String yamlBlock = content.substring(yamlStart, closingPos).strip();
        if (yamlBlock.isEmpty()) {
            return Map.of();
        }

        try {
            Map<String, Object> result = YAML.readValue(yamlBlock, MAP_TYPE);
            return result != null ? result : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Returns the body of the document (everything after the closing {@code ---}),
     * or the full content when no frontmatter is present.
     */
    public String body(String content) {
        if (content == null || !content.startsWith(DELIMITER)) {
            return content != null ? content : "";
        }

        int yamlStart = DELIMITER.length();
        if (yamlStart < content.length() && content.charAt(yamlStart) != '\n' && content.charAt(yamlStart) != '\r') {
            return content;
        }

        int closingPos = content.indexOf('\n' + DELIMITER, yamlStart);
        if (closingPos == -1) {
            return content;
        }

        int bodyStart = closingPos + 1 + DELIMITER.length();
        return bodyStart < content.length() ? content.substring(bodyStart) : "";
    }
}
