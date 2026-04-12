package brain.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Extracts structured wiki knowledge from a session transcript (RF-CAPTURE-03–05).
 *
 * <p>Uses Claude Haiku via Spring AI structured output to minimise cost (RNF-COST-02).
 * Transcripts below {@link #MIN_TOKENS} estimated tokens are skipped without an API call.
 */
@Service
public class ExtractorService {

    static final int MIN_TOKENS = 500;

    private static final String SYSTEM_PROMPT = """
        You are a knowledge extraction assistant for a personal wiki (second brain).
        Analyse the session transcript and identify knowledge worth persisting.

        RULES:
        1. Only suggest pages for concepts/entities that appear meaningfully in the transcript.
        2. Prefer updating existing pages over creating duplicate ones.
        3. pageId must be lowercase kebab-case with a category prefix, e.g. concepts/spring-ai.
        4. type must be one of: concept, entity, source, decision, question.
        5. Be concise: each summary is a single sentence (max 20 words).
        6. Return an empty list for any field where there is nothing to report.
        """;

    private final ChatClient chatClient;

    public ExtractorService(@Qualifier("extractor") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Extracts structured wiki candidates from {@code sourceContent}.
     *
     * @param sourceContent the session transcript or source text
     * @param wikiIndex     the current wiki index (may be null or empty)
     * @return extraction result, or {@link ExtractionResult#empty()} if content is too short
     */
    public ExtractionResult extract(String sourceContent, String wikiIndex) {
        if (estimateTokens(sourceContent) < MIN_TOKENS) {
            return ExtractionResult.empty();
        }
        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(buildPrompt(sourceContent, wikiIndex))
            .call()
            .entity(ExtractionResult.class);
    }

    /**
     * Rough token estimate: 1 token ≈ 4 characters.
     */
    static int estimateTokens(String content) {
        return content == null ? 0 : content.length() / 4;
    }

    private String buildPrompt(String sourceContent, String wikiIndex) {
        var sb = new StringBuilder();
        if (wikiIndex != null && !wikiIndex.isBlank()) {
            sb.append("## Existing wiki index\n").append(wikiIndex).append("\n\n");
        }
        sb.append("## Session transcript\n").append(sourceContent);
        return sb.toString();
    }
}
