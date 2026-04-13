package brain.ai;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    @Qualifier("wikiWriter")
    public ChatClient wikiWriterChatClient(
            AnthropicChatModel chatModel,
            @Value("${brain.ai.wiki-write-model:claude-sonnet-4-6}") String model) {
        return ChatClient.builder(chatModel)
            .defaultOptions(AnthropicChatOptions.builder().model(model).build())
            .build();
    }

    @Bean
    @Qualifier("extractor")
    public ChatClient extractorChatClient(
            AnthropicChatModel chatModel,
            @Value("${brain.ai.extraction-model:claude-haiku-4-5-20251001}") String model) {
        return ChatClient.builder(chatModel)
            .defaultOptions(AnthropicChatOptions.builder().model(model).build())
            .build();
    }

    @Bean
    @Qualifier("query")
    public ChatClient queryChatClient(
            AnthropicChatModel chatModel,
            @Value("${brain.ai.query-model:claude-sonnet-4-6}") String model) {
        return ChatClient.builder(chatModel)
            .defaultOptions(AnthropicChatOptions.builder().model(model).build())
            .build();
    }

    @Bean
    @Qualifier("lintSemantic")
    public ChatClient lintSemanticChatClient(
            AnthropicChatModel chatModel,
            @Value("${brain.ai.lint-model:claude-haiku-4-5-20251001}") String model) {
        return ChatClient.builder(chatModel)
            .defaultOptions(AnthropicChatOptions.builder().model(model).build())
            .build();
    }
}
