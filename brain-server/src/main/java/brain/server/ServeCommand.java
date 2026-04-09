package brain.server;

import org.springframework.boot.SpringApplication;
import picocli.CommandLine.Command;

@Command(
    name = "serve",
    description = "Start the MCP brain server (stdio transport)"
)
public class ServeCommand implements Runnable {

    @Override
    public void run() {
        validateApiKey();
        SpringApplication.run(BrainApplication.class);
    }

    private void validateApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("[brain] ERROR: ANTHROPIC_API_KEY environment variable is not set.");
            System.err.println("[brain] Hint: export ANTHROPIC_API_KEY=sk-ant-...");
            System.exit(1);
        }
    }
}
