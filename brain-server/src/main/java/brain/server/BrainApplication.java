package brain.server;

import brain.server.cli.BrainCli;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@SpringBootApplication(scanBasePackages = "brain")
public class BrainApplication {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BrainCli()).execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
        // On success, let the JVM exit naturally so async Virtual Threads can complete.
    }
}
