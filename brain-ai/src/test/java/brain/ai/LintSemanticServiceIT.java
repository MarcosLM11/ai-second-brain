package brain.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link LintSemanticService}.
 * Requires a real {@code ANTHROPIC_API_KEY} and makes a live Haiku API call.
 * Skipped automatically when the key is absent.
 */
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class LintSemanticServiceIT {

    private LintSemanticService buildService() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        var api = AnthropicApi.builder().apiKey(apiKey).build();
        var chatModel = AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(AnthropicChatOptions.builder().model("claude-haiku-4-5-20251001").build())
            .build();
        return new LintSemanticService(ChatClient.builder(chatModel).build());
    }

    @Test
    void detectsDuplicateTitles() {
        var titles = List.of(
            "JWT", "JSON Web Token", "OAuth", "Autenticación OAuth2",
            "Spring Boot", "Docker", "Kubernetes", "K8s"
        );
        String contentSample = """
            JWT es un estándar para tokens de acceso. JSON Web Token permite autenticar usuarios.
            OAuth es un protocolo de autorización. OAuth2 es la versión actual de OAuth.
            Kubernetes (K8s) es un orquestador de contenedores. Docker crea imágenes de contenedores.
            """;

        SemanticReport report = buildService().analyse(titles, contentSample);

        assertThat(report).isNotNull();
        assertThat(report.duplicatePairs())
            .as("Should detect JWT / JSON Web Token as likely duplicates")
            .anyMatch(pair -> pair.contains("JWT") && pair.contains("JSON Web Token"));
    }

    @Test
    void detectsConceptsWithoutPage() {
        var titles = List.of("Spring Boot", "Docker");
        String contentSample = """
            Spring Boot se usa con Maven y Gradle. Docker corre sobre Linux con cgroups y namespaces.
            Para desplegar necesitamos un CI/CD pipeline. Jenkins o GitHub Actions son opciones.
            Los microservicios se comunican vía REST o gRPC. El API Gateway enruta las peticiones.
            """;

        SemanticReport report = buildService().analyse(titles, contentSample);

        assertThat(report.conceptsWithoutPage()).isNotEmpty();
    }

    @Test
    void generatesThreeToFiveGapQuestions() {
        var titles = List.of("Spring AI", "Claude", "LLM", "Prompt Engineering");
        String contentSample = """
            Spring AI abstrae modelos LLM como Claude de Anthropic. Los prompts se diseñan
            con técnicas específicas para obtener mejores respuestas. RAG es un patrón común.
            Los embeddings permiten búsqueda semántica. Los tokens son la unidad de procesamiento.
            """;

        SemanticReport report = buildService().analyse(titles, contentSample);

        assertThat(report.gapQuestions())
            .as("Should generate between 3 and 5 gap questions")
            .hasSizeBetween(3, 5);
    }
}
