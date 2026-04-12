package brain.ai;

import java.util.List;

/**
 * Structured output from {@link ExtractorService#extract}.
 *
 * @param newPages       new wiki pages to create
 * @param pageUpdates    existing wiki pages that need updating
 * @param contradictions factual contradictions found between the source and existing wiki
 * @param suggestedLinks wikilinks that should be created between existing pages
 */
public record ExtractionResult(
    List<PageCandidate> newPages,
    List<PageUpdate> pageUpdates,
    List<String> contradictions,
    List<String> suggestedLinks
) {
    public static ExtractionResult empty() {
        return new ExtractionResult(List.of(), List.of(), List.of(), List.of());
    }
}
