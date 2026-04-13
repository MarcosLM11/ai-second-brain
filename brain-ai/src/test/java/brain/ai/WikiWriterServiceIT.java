package brain.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link WikiWriterService}.
 * Requires a real {@code ANTHROPIC_API_KEY}. Skipped when absent.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class WikiWriterServiceIT {

    private WikiWriterService buildService() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        var api = AnthropicApi.builder().apiKey(apiKey).build();
        var model = AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(AnthropicChatOptions.builder().model("claude-haiku-4-5-20251001").build())
            .build();
        return new WikiWriterService(ChatClient.builder(model).build());
    }

    @Test
    void generatesPageWithValidFrontmatter() {
        String content = buildService().writePage(
            "Create a wiki page titled \"Spring AI\". Summary: Spring framework for AI/LLM integration.",
            null, null, null
        );

        assertThat(content).isNotBlank();
        assertThat(content).contains("---");
        assertThat(content).contains("title:");
        assertThat(content).contains("type:");
    }

    @Test
    void generatesAtLeastOneWikilink() {
        String content = buildService().writePage(
            "Create a wiki page about JWT (JSON Web Tokens) for authentication.",
            null, null, null
        );

        assertThat(content)
            .as("Page must contain at least one [[wikilink]]")
            .containsPattern("\\[\\[.+]]");
    }
}
