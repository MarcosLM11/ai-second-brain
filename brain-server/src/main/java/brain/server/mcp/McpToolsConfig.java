package brain.server.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolsConfig {

    @Bean
    public ToolCallbackProvider brainTools(
            WikiTools wikiTools, SchemaTools schemaTools, LogTools logTools, IngestTools ingestTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(wikiTools, schemaTools, logTools, ingestTools)
            .build();
    }
}
