Ingest a source (URL or local .md/.txt file) into the second brain wiki.

Source: $ARGUMENTS

Follow these steps exactly, in order:

## 1. Read schema
Call `schema_read` to load the wiki taxonomy (node types, frontmatter fields, wikilink conventions).

## 2. Hash and cache check
Call `source_hash` with the source identifier (the URL or file path).
Then call `cache_check` with that hash. The response is JSON:
- `{"hit":false}` → continue to step 3.
- `{"hit":true,"lastProcessed":"<ISO-8601>"}` → stop immediately. Report to the user:
  "Ya procesada el <lastProcessed>. No se crearán páginas duplicadas." Do not proceed further.

## 3. Fetch content
- If the source starts with `https://`: call `fetch_url`.
- If the source is a local path ending in `.md` or `.txt`: call `fetch_file`.
- If `fetch_url` or `fetch_file` returns an ERROR string: stop and report the error to the user.

## 4. Read wiki index
Call `wiki_index_read` to get the current list of existing pages and their titles.

## 5. Extract concepts and entities
Analyse the fetched content carefully. Identify:
- **Concepts**: technical or domain terms worth a standalone wiki page (type: concept)
- **Entities**: people, projects, tools, organisations (type: entity)
- **One source page**: a summary page for this source itself (type: source)

For each concept/entity: check the wiki index from step 4 to see if a page already exists.
- If it exists: plan to UPDATE it (merge new information, do not lose existing content).
- If it does not exist: plan to CREATE it.

Aim for 3–8 pages per source. Prioritise depth over breadth.

## 6. Write wiki pages
For each page to create or update:

a. If updating: call `wiki_read` to load the existing page content.

b. Call `wiki_write` with:
   - `pageId`: lowercase kebab-case path, e.g. `concepts/spring-ai` or `entities/anthropic`
   - `content`: valid markdown with YAML frontmatter following the schema exactly.

Requirements for each page:
- Frontmatter must have: `title`, `type`, `tags`, `sources`, `created`, `updated` (YYYY-MM-DD).
- Body must have at least one `[[wikilink]]` to a related concept, entity, or the source page.
- Mark any statement inferred (not explicit in the source) with `*[inferred]*`.
- Write all content in Spanish (unless the schema specifies otherwise).
- The source page must include a `url:` frontmatter field and a "## Conceptos clave extraídos" section listing all related pages created.

## 7. Update index.md
Read the current `index.md` via `wiki_index_read`.
Append entries for all newly created pages in this format:
```
| [title](pageId) | type | one-line summary |
```
Write the updated content via `wiki_write` with pageId `index`.

## 8. Log the ingestion
Call `log_append` with a summary entry, e.g.:
`Ingested: <source> → pages: <list of pageIds written>`

## 9. Cache the source
Call `cache_set` with:
- `sha256`: the hash from step 2
- `metadata`: a JSON string like `{"source":"<url-or-path>","pages":["<pageId>",...],"date":"<YYYY-MM-DD>"}`

## 10. Build knowledge graph
Call `graph_build` with `force=false`.
This updates the graph incrementally with the new wiki pages written in step 6.

## 11. Report to user
Summarise what was done:
- Pages created (with pageIds)
- Pages updated (with pageIds)
- Confirm log and cache entries written
- Confirm graph updated (nodes and edges from `graph_build` output)
