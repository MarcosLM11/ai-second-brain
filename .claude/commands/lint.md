Lint the wiki for structural and semantic issues, then generate a HEALTH_REPORT.md.

Follow these steps exactly, in order:

## 1. Structural lint (automatic)

Call `lint_structural`.

This detects:
- Orphaned pages (no incoming wikilinks)
- Broken wikilinks ([[references]] pointing to non-existent pages)
- Asymmetric backlinks (A → B exists but B → A does not)

Display the results to the user in a readable format:
- If there are 0 problems in all three categories: report "✅ 0 problemas estructurales encontrados."
- Otherwise: list each category with its count and the affected pages/links.

## 2. Offer semantic analysis

After showing the structural results, ask the user:
> ¿Quieres ejecutar también el análisis semántico? Detecta posibles páginas duplicadas, conceptos sin página y gaps de conocimiento. Requiere una llamada a Claude Haiku (~5s). (sí/no)

**If the user says no:** skip to step 4 (write report with structural data only).

**If the user says yes:** continue with step 3.

## 3. Semantic lint (optional, with confirmation)

Call `lint_semantic`.

This uses Claude Haiku to detect:
- Pairs of pages that may be duplicates (similar titles)
- Concepts mentioned frequently but lacking a dedicated page
- 3–5 questions whose answers would fill knowledge gaps

Display the semantic results to the user:
- Duplicate pairs (if any)
- Concepts without page (if any)
- Gap questions (always 3–5)

## 4. Generate HEALTH_REPORT.md

Call `wiki_write` with `pageId = "HEALTH_REPORT"` and the following content structure:

```markdown
---
title: "Health Report"
type: report
tags: [lint, health]
created: <today YYYY-MM-DD>
updated: <today YYYY-MM-DD>
---

# Wiki Health Report — <today YYYY-MM-DD>

## Structural Analysis

**Orphaned pages:** <count>
<list of orphaned page IDs, or "Ninguna">

**Broken wikilinks:** <count>
<list of "[[target]] in source", or "Ninguno">

**Asymmetric backlinks:** <count>
<list of "A → B (no return link)", or "Ninguno">

## Semantic Analysis
<If semantic lint was run:>
**Possible duplicate pages:** <count>
<list of "PageA | PageB" pairs, or "Ninguno">

**Concepts without dedicated page:** <count>
<list, or "Ninguno">

**Knowledge gap questions:**
1. <question>
2. <question>
...

<If semantic lint was NOT run:>
*Semantic analysis not executed in this run.*
```

## 5. Log the lint run

Call `log_append` with:
`lint: <N_orphans> huérfanas, <N_broken> wikilinks rotos, <N_asymmetric> backlinks asimétricos<, semántico: <N_dup> pares duplicados, <N_concepts> conceptos sin página> (HEALTH_REPORT.md actualizado)`

## 6. Report to user

Summarise the full lint run:
- Total issues found (structural + semantic if run)
- Confirm HEALTH_REPORT.md written
- Suggest top 1–2 actions to improve wiki health (e.g. "Considera fusionar [[JWT]] y [[JSON Web Token]]")
