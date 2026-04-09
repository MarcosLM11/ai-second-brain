package brain.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@SpringBootApplication
@Command(
    name = "brain",
    subcommands = {ServeCommand.class},
    mixinStandardHelpOptions = true,
    description = "AI Second Brain — personal knowledge system"
)
public class BrainApplication implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new BrainApplication()).execute(args));
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}
