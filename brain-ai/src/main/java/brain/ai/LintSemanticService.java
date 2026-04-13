package brain.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Semantic wiki analysis using Claude Haiku (RF-LINT-04–06).
 *
 * <p>Detects potential duplicate pages by title similarity, identifies concepts
 * mentioned frequently without a dedicated page, and suggests gap-filling questions.
 * Uses only the page title index and a sample of page content — no embeddings required.
 */
@Service
public class LintSemanticService {

    private static final String SYSTEM_PROMPT = """
        Eres un asistente de análisis de wikis personales. Recibes una lista de títulos
        de páginas wiki y un resumen de contenido. Tu tarea es devolver un análisis
        estructurado con tres secciones EXACTAS:

        DUPLICADOS:
        (lista de pares "Título A | Título B" que probablemente son duplicados, uno por línea)

        CONCEPTOS_SIN_PÁGINA:
        (lista de conceptos mencionados frecuentemente en el contenido que no tienen página propia, uno por línea)

        PREGUNTAS_GAP:
        (entre 3 y 5 preguntas cuyas respuestas llenarían gaps de conocimiento, una por línea)

        Si no hay duplicados, escribe "ninguno" bajo DUPLICADOS.
        Si no hay conceptos sin página, escribe "ninguno" bajo CONCEPTOS_SIN_PÁGINA.
        Escribe EXACTAMENTE entre 3 y 5 preguntas bajo PREGUNTAS_GAP.
        """;

    private static final Pattern SECTION_DUPLICATES  = Pattern.compile(
        "DUPLICADOS:\\s*\\n(.*?)(?=\\nCONCEPTOS_SIN_PÁGINA:|$)", Pattern.DOTALL);
    private static final Pattern SECTION_CONCEPTS    = Pattern.compile(
        "CONCEPTOS_SIN_PÁGINA:\\s*\\n(.*?)(?=\\nPREGUNTAS_GAP:|$)", Pattern.DOTALL);
    private static final Pattern SECTION_QUESTIONS   = Pattern.compile(
        "PREGUNTAS_GAP:\\s*\\n(.*)", Pattern.DOTALL);

    private final ChatClient chatClient;

    public LintSemanticService(@Qualifier("lintSemantic") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Analyses the wiki semantically.
     *
     * @param pageTitles list of all page IDs / titles in the wiki
     * @param contentSample concatenated sample of page content (first ~200 chars per page)
     * @return {@link SemanticReport} with duplicates, concepts without page, and gap questions
     */
    public SemanticReport analyse(List<String> pageTitles, String contentSample) {
        String userMessage = buildUserMessage(pageTitles, contentSample);
        String response = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userMessage)
            .call()
            .content();

        return parse(response);
    }

    private String buildUserMessage(List<String> pageTitles, String contentSample) {
        return """
            ## Páginas del wiki (%d páginas)
            %s

            ## Muestra de contenido
            %s
            """.formatted(
            pageTitles.size(),
            String.join("\n", pageTitles),
            contentSample == null ? "" : contentSample
        );
    }

    private SemanticReport parse(String response) {
        List<String> duplicates = extractSection(response, SECTION_DUPLICATES);
        List<String> concepts   = extractSection(response, SECTION_CONCEPTS);
        List<String> questions  = extractSection(response, SECTION_QUESTIONS);
        return new SemanticReport(duplicates, concepts, questions);
    }

    private List<String> extractSection(String response, Pattern pattern) {
        Matcher m = pattern.matcher(response);
        if (!m.find()) return List.of();
        String block = m.group(1).trim();
        if (block.equalsIgnoreCase("ninguno") || block.isBlank()) return List.of();
        return Arrays.stream(block.split("\\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());
    }
}
