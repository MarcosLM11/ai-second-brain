package brain.ai;

import java.util.List;

/**
 * Result of a semantic lint analysis of the wiki.
 *
 * @param duplicatePairs     pairs of page IDs that are likely duplicates, formatted as "pageA | pageB"
 * @param conceptsWithoutPage concepts mentioned frequently that lack a dedicated wiki page
 * @param gapQuestions       3–5 questions whose answers would fill knowledge gaps
 */
public record SemanticReport(
    List<String> duplicatePairs,
    List<String> conceptsWithoutPage,
    List<String> gapQuestions
) {}
