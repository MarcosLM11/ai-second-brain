package brain.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class WikiWriterService {

    private static final String SYSTEM_PROMPT = """
        You are a precise knowledge base writer for an Obsidian-style personal wiki.

        RULES:
        1. Output ONLY valid markdown with YAML frontmatter. No prose outside the page itself.
        2. Follow the schema structure exactly: required frontmatter fields, section headings.
        3. Use [[wikilinks]] to reference related concepts. At least one [[wikilink]] is required.
        4. Mark any statement you infer (not explicitly stated in the input) with *[inferred]*.
        5. Never invent facts. Only use information from the instruction and context provided.
        6. Dates in frontmatter use YYYY-MM-DD format. Today is %s.
        7. Write page content in the language specified by the schema (default: Spanish).
        """;

    private final ChatClient chatClient;

    public WikiWriterService(@Qualifier("wikiWriter") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String writePage(String instruction, String schema, String existingContent, String relatedContext) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT.formatted(LocalDate.now()))
            .user(buildUserPrompt(instruction, schema, existingContent, relatedContext))
            .call()
            .content();
    }

    private String buildUserPrompt(String instruction, String schema,
                                    String existingContent, String relatedContext) {
        var sb = new StringBuilder();
        sb.append("## Instruction\n").append(instruction).append("\n\n");
        if (schema != null && !schema.isBlank()) {
            sb.append("## Schema\n").append(schema).append("\n\n");
        }
        if (existingContent != null && !existingContent.isBlank()) {
            sb.append("## Existing content (update — do not lose information)\n")
              .append(existingContent).append("\n\n");
        }
        if (relatedContext != null && !relatedContext.isBlank()) {
            sb.append("## Related context (use for [[wikilinks]])\n")
              .append(relatedContext).append("\n\n");
        }
        sb.append("Now write the wiki page:");
        return sb.toString();
    }
}
