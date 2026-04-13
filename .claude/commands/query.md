Answer a question from the second brain wiki, then offer to archive the response.

Question: $ARGUMENTS

Follow these steps exactly, in order:

## 1. Find relevant pages

**First**, call `wiki_index_read` to get the total page count (count the lines in the index).

**If the wiki has ≤ 100 pages (small wiki — standard flow):**
Call `graph_query` with the question text and `maxPages: 8`.
- If it returns an empty list: pick up to 8 pages from the index whose title or summary seems relevant. If there are still no relevant pages, answer "No hay páginas en la wiki relacionadas con esta pregunta." and stop.

**If the wiki has > 100 pages (large wiki — FTS5 pre-filter):**
Call `search` with the question text and `limit: 15` to get the most relevant page IDs via BM25.
- If `search` returns an empty list, fall back to `graph_query` with the question text and `maxPages: 8`.
- Take the top 8 page IDs from the search results.

## 2. Read the pages
For each page ID returned (up to 8), call `wiki_read`.
Discard any that return "Page not found".

## 3. Synthesise the answer
Using ONLY the content from the pages you just read:
- Write a clear, concise answer.
- Cite every claim with a `[[wikilink]]` referencing the source page (use the page's `title` field as the link text if helpful).
- If the collected context is insufficient to fully answer the question, say so explicitly and state what is known.
- At least one `[[wikilink]]` is required.

Present the answer to the user now.

## 4. Offer to archive (file-back)
After presenting the answer, ask the user:
> ¿Quieres archivar esta respuesta en la wiki? (sí/no)

**If the user says no (or does not respond):** call `log_append` with:
`Query: "<question>"`
Then stop.

**If the user says yes:** continue with steps 5–7.

## 5. Write the question page
Generate a `pageId` using the format `questions/<slug>` where `<slug>` is a lowercase kebab-case summary of the question (max 5 words), e.g. `questions/que-es-spring-ai`.

Call `wiki_write` with:
- `pageId`: the generated ID above
- `content`: valid markdown with YAML frontmatter:

```yaml
---
title: "<the question, verbatim>"
type: question
tags: []
sources: []
created: <today YYYY-MM-DD>
updated: <today YYYY-MM-DD>
---
```

Followed by:
- `# <question as heading>`
- The full synthesised answer (with `[[wikilinks]]`)
- A `## Related` section linking to each source page used

## 6. Update wikilinks in source pages
For each source page read in step 2 that does NOT already link to the new question page:
- Call `wiki_read` to get its current content.
- If the page has a `## Related` section, append `- [[<question pageId>]] — pregunta archivada`.
- If it does not have a `## Related` section, add one at the end.
- Call `wiki_write` with the updated content.

Skip this step if more than 3 source pages would need updating (to avoid large side effects).

## 7. Log the query
Call `log_append` with:
`Query: "<question>" → [[<pageId>]] (filed)`
