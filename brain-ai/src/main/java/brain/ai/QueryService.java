package brain.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Synthesizes answers from wiki context using a ChatClient (RF-QUERY-01–05).
 *
 * <p>Caller is responsible for gathering relevant wiki pages and passing
 * their concatenated content as {@code wikiContext}.
 */
@Service
public class QueryService {

    private static final String SYSTEM_PROMPT = """
        Eres un asistente de conocimiento personal. Sintetizas respuestas precisas
        a partir de páginas wiki proporcionadas como contexto.

        REGLAS:
        1. Basa tu respuesta SOLO en el contexto wiki proporcionado. No inventes hechos.
        2. Cita cada afirmación con [[wikilink]] que referencie la página fuente.
        3. Si el contexto es insuficiente para responder, dilo explícitamente.
        4. Sé conciso pero completo. Usa bullet points al listar varios elementos.
        5. Escribe en el mismo idioma que la pregunta (por defecto: español).
        6. Al menos un [[wikilink]] de cita es obligatorio por respuesta.
        """;

    private final ChatClient chatClient;

    public QueryService(@Qualifier("query") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Synthesizes an answer to {@code question} from the provided wiki context.
     *
     * @param question    the user's question
     * @param wikiContext concatenated markdown content of relevant wiki pages
     * @return synthesized answer with [[wikilink]] citations
     */
    public String query(String question, String wikiContext) {
        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(buildUserPrompt(question, wikiContext))
            .call()
            .content();
    }

    private String buildUserPrompt(String question, String wikiContext) {
        var sb = new StringBuilder();
        if (wikiContext != null && !wikiContext.isBlank()) {
            sb.append("## Contexto wiki\n").append(wikiContext).append("\n\n");
        }
        sb.append("## Pregunta\n").append(question).append("\n\n");
        sb.append("Responde con citas [[wikilink]] a las páginas fuente:");
        return sb.toString();
    }
}
