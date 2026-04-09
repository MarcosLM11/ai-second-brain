package brain.server.cli;

import brain.server.BrainApplication;
import org.springframework.boot.SpringApplication;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "serve",
    description = "Start the MCP brain server in stdio mode"
)
public class ServeCommand implements Runnable {

    @ParentCommand
    BrainCli parent;

    @Override
    public void run() {
        validateApiKey();
        applyConfig();
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

    private void applyConfig() {
        if (parent.configFile != null) {
            System.setProperty("brain.config.path", parent.configFile.toAbsolutePath().toString());
        }
    }
}
