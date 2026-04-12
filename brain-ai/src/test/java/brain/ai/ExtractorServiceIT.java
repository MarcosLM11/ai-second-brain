package brain.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ExtractorService}.
 * Requires a real {@code ANTHROPIC_API_KEY} and makes a live API call.
 * Skipped automatically when the key is absent.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ExtractorServiceIT {

    private static final String SAMPLE_TRANSCRIPT = """
        Hoy trabajé en la implementación de Spring AI para mi segundo cerebro personal.
        Spring AI es un framework de Spring que abstrae los modelos LLM con una API común.
        Usé Claude Haiku como modelo de extracción por ser más económico y rápido.
        La implementación usa structured output con BeanOutputConverter para deserializar JSON.
        También configuré el caché SQLite para evitar reprocesar fuentes ya ingestadas.
        El sistema detecta automáticamente cuando el contenido es demasiado corto para procesar.
        Anthropic es la empresa detrás de Claude. Claude 4.5 Haiku es el modelo más económico.
        La arquitectura usa brain-core, brain-ai, brain-wiki, brain-graph y brain-server como módulos.
        Spring Boot orquesta todos los módulos como un MCP server que Claude Code puede invocar.
        El proyecto se llama ai-second-brain y está en GitHub bajo el usuario MarcosLM11.
        Implementé CacheStoreSqlite para persistir qué fuentes han sido procesadas ya.
        Los criterios de aceptación incluyen tests unitarios sin red y un integration test real.
        """;

    private ExtractorService buildService() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        var api = AnthropicApi.builder().apiKey(apiKey).build();
        var chatModel = AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(AnthropicChatOptions.builder().model("claude-haiku-4-5-20251001").build())
            .build();
        return new ExtractorService(ChatClient.builder(chatModel).build());
    }

    @Test
    void realTranscriptProducesAtLeastOnePageCandidateOrUpdate() {
        ExtractionResult result = buildService().extract(SAMPLE_TRANSCRIPT, null);

        assertThat(result).isNotNull();
        boolean hasContent = !result.newPages().isEmpty() || !result.pageUpdates().isEmpty();
        assertThat(hasContent)
            .as("Expected at least one PageCandidate or PageUpdate but got: %s", result)
            .isTrue();
    }

    @Test
    void resultDeserializesCorrectly() {
        ExtractionResult result = buildService().extract(SAMPLE_TRANSCRIPT, null);

        assertThat(result.newPages()).isNotNull();
        assertThat(result.pageUpdates()).isNotNull();
        assertThat(result.contradictions()).isNotNull();
        assertThat(result.suggestedLinks()).isNotNull();

        result.newPages().forEach(page -> {
            assertThat(page.pageId()).isNotBlank();
            assertThat(page.title()).isNotBlank();
            assertThat(page.type()).isNotBlank();
        });
    }
}
