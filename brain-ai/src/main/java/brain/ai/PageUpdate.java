package brain.ai;

/**
 * An existing wiki page that should be updated with new information.
 *
 * @param pageId  the existing page's kebab-case id, e.g. {@code concepts/spring-ai}
 * @param summary description of the new information to merge into the page
 */
public record PageUpdate(String pageId, String summary) {}
