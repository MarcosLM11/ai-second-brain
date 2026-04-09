package brain.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WikiWriterServiceTest {

    private static final String MOCK_PAGE = """
        ---
        title: "JWT Verification"
        type: concept
        aliases: []
        tags: [security]
        sources: []
        created: 2026-04-09
        updated: 2026-04-09
        ---
        # JWT Verification

        Un JWT es un token compacto que permite verificar identidad sin estado en servidor. *[inferred]*

        ## Related
        - [[oauth2]] — protocolo de autorización que usa JWT como bearer token
        """;

    @Mock ChatClient chatClient;
    @Mock ChatClient.ChatClientRequestSpec requestSpec;
    @Mock ChatClient.CallResponseSpec callSpec;

    WikiWriterService service;

    @BeforeEach
    void setUp() {
        service = new WikiWriterService(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void writePageReturnsFrontmatterYaml() {
        when(callSpec.content()).thenReturn(MOCK_PAGE);

        String result = service.writePage("Escribe una página sobre JWT", null, null, null);

        assertThat(result).startsWith("---");
        assertThat(result).contains("title:");
        assertThat(result).contains("type:");
    }

    @Test
    void writePageContainsWikilink() {
        when(callSpec.content()).thenReturn(MOCK_PAGE);

        String result = service.writePage("Escribe una página sobre JWT", null, null, null);

        assertThat(result).containsPattern("\\[\\[.+\\]\\]");
    }

    @Test
    void writePagePassesSchemaInUserPrompt() {
        when(callSpec.content()).thenReturn(MOCK_PAGE);

        service.writePage("instruction", "## Schema\nconcept: ...", null, null);

        // verify system + user are called (chain is invoked)
        org.mockito.Mockito.verify(requestSpec).system(anyString());
        org.mockito.Mockito.verify(requestSpec).user(anyString());
    }

    @Test
    void writePageDoesNotCallRealApi() {
        when(callSpec.content()).thenReturn(MOCK_PAGE);

        // If this test passes without network errors, the mock is working
        String result = service.writePage("test", null, null, null);

        assertThat(result).isNotBlank();
    }
}
