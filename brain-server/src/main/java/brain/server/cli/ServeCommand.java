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
        applyConfig();
        SpringApplication.run(BrainApplication.class);
    }

    private void applyConfig() {
        if (parent.configFile != null) {
            System.setProperty("brain.config.path", parent.configFile.toAbsolutePath().toString());
        }
    }
}
