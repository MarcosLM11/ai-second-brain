package brain.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    private static final String MOCK_ANSWER = """
        [[Spring AI]] es un framework de Spring que facilita la integración con LLMs.
        Según [[spring-ai-overview]], abstrae los proveedores de modelos con una API común.
        Se puede usar con [[Anthropic]] para acceder a Claude.
        """;

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    QueryService service;

    @BeforeEach
    void setUp() {
        service = new QueryService(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void queryReturnsAnswerWithWikilinks() {
        when(callSpec.content()).thenReturn(MOCK_ANSWER);

        String result = service.query("¿Qué es Spring AI?", "## Spring AI\n...");

        assertThat(result).containsPattern("\\[\\[.+\\]\\]");
    }

    @Test
    void queryWithoutContextStillCallsApi() {
        when(callSpec.content()).thenReturn(MOCK_ANSWER);

        String result = service.query("¿Qué es un JWT?", null);

        assertThat(result).isNotBlank();
        verify(requestSpec).system(anyString());
        verify(requestSpec).user(anyString());
    }

    @Test
    void queryIncludesQuestionInUserPrompt() {
        when(callSpec.content()).thenReturn(MOCK_ANSWER);

        service.query("¿Qué es Spring AI?", "contexto wiki");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(captor.capture());
        assertThat(captor.getValue())
            .contains("¿Qué es Spring AI?")
            .contains("contexto wiki");
    }

    @Test
    void queryDoesNotCallRealApi() {
        when(callSpec.content()).thenReturn(MOCK_ANSWER);

        String result = service.query("pregunta de prueba", "contexto");

        assertThat(result).isNotBlank();
    }
}
