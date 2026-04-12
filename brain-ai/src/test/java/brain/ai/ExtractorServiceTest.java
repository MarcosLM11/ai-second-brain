package brain.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtractorServiceTest {

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    ExtractorService service;

    @BeforeEach
    void setUp() {
        service = new ExtractorService(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void shortContentReturnsEmptyWithoutCallingApi() {
        String shortContent = "Hello world"; // well under 500 tokens (~3 tokens)

        ExtractionResult result = service.extract(shortContent, null);

        assertThat(result.newPages()).isEmpty();
        assertThat(result.pageUpdates()).isEmpty();
        assertThat(result.contradictions()).isEmpty();
        assertThat(result.suggestedLinks()).isEmpty();
        verifyNoInteractions(chatClient);
    }

    @Test
    void contentExactlyAtThresholdIsProcessed() {
        // 500 tokens * 4 chars/token = 2000 chars exactly
        String content = "a".repeat(ExtractorService.MIN_TOKENS * 4);
        var expected = new ExtractionResult(
            List.of(new PageCandidate("concepts/test", "Test", "concept", "A test page")),
            List.of(),
            List.of(),
            List.of()
        );
        when(callSpec.entity(ExtractionResult.class)).thenReturn(expected);

        ExtractionResult result = service.extract(content, null);

        assertThat(result.newPages()).hasSize(1);
        verify(chatClient).prompt();
    }

    @Test
    void longContentCallsApiAndDeserializesResult() {
        String content = "a".repeat(3000); // ~750 tokens
        var expected = new ExtractionResult(
            List.of(new PageCandidate("concepts/foo", "Foo", "concept", "About foo")),
            List.of(new PageUpdate("concepts/bar", "Update bar with foo context")),
            List.of("Contradiction: X says A but transcript says B"),
            List.of("[[foo]] → [[bar]]")
        );
        when(callSpec.entity(ExtractionResult.class)).thenReturn(expected);

        ExtractionResult result = service.extract(content, "## index");

        assertThat(result.newPages()).hasSize(1);
        assertThat(result.newPages().get(0).pageId()).isEqualTo("concepts/foo");
        assertThat(result.pageUpdates()).hasSize(1);
        assertThat(result.contradictions()).hasSize(1);
        assertThat(result.suggestedLinks()).hasSize(1);
    }

    @Test
    void nullContentReturnsEmpty() {
        ExtractionResult result = service.extract(null, null);

        assertThat(result).isEqualTo(ExtractionResult.empty());
        verifyNoInteractions(chatClient);
    }

    @Test
    void estimateTokensApproximatesFourCharsPerToken() {
        assertThat(ExtractorService.estimateTokens("a".repeat(400))).isEqualTo(100);
        assertThat(ExtractorService.estimateTokens("a".repeat(2000))).isEqualTo(500);
        assertThat(ExtractorService.estimateTokens(null)).isEqualTo(0);
    }
}
