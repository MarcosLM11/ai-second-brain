package brain.server.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(
    name = "brain",
    subcommands = {ServeCommand.class},
    mixinStandardHelpOptions = true,
    version = "brain 1.0.0",
    description = "AI Second Brain — personal knowledge management system"
)
public class BrainCli implements Runnable {

    @Option(
        names = {"--config", "-c"},
        description = "Path to brain.toml configuration file",
        paramLabel = "FILE"
    )
    Path configFile;

    @Override
    public void run() {
        new CommandLine(this).usage(System.err);
    }
}
