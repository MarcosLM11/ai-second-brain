package brain.server;

import brain.server.cli.BrainCli;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication(scanBasePackages = "brain")
public class BrainApplication {

    public static void main(String[] args) {
        System.exit(new CommandLine(new BrainCli()).execute(args));
    }
}
