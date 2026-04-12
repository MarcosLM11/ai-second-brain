package brain.ai;

/**
 * A new wiki page to create as a result of content extraction.
 *
 * @param pageId  kebab-case path, e.g. {@code concepts/spring-ai}
 * @param title   human-readable page title
 * @param type    wiki page type: concept, entity, source, decision, question
 * @param summary one-sentence description of the page content
 */
public record PageCandidate(String pageId, String title, String type, String summary) {}
