package brain.server.mcp;

import brain.wiki.LogAppender;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class LogTools {

    private final LogAppender logAppender;

    public LogTools(LogAppender logAppender) {
        this.logAppender = logAppender;
    }

    @Tool(description = "Append an entry to log.md with an ISO-8601 timestamp. Use to record sessions, decisions, or events.")
    public String log_append(
        @ToolParam(description = "Entry text to append to the log") String entry
    ) {
        logAppender.append(entry);
        return "Logged: " + entry;
    }
}
