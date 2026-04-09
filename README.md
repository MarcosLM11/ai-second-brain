# 🧠 AI Second Brain

> A personal knowledge system that **compiles knowledge once and keeps it updated** — not a RAG that rediscovers from scratch on every query.

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1-6DB33F?logo=spring)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## The Problem

Knowledge gets lost. Personal wikis get abandoned because maintenance grows faster than value. Conventional RAG systems (NotebookLM, ChatGPT uploads) rediscover your knowledge from scratch on every query — they accumulate nothing.

## The Idea

A **personal second brain** where an LLM does the maintenance work — writing, linking, updating, detecting contradictions — while you contribute sources, ask questions, and navigate results in Obsidian.

> *Obsidian is the IDE. The LLM is the programmer. The wiki is the codebase.*

Every source you ingest makes the whole wiki richer. Every question you ask can be archived back into it. The knowledge graph grows more valuable the more you use it.

---

## How It Works

The system is built around three physical layers:

```
┌─────────────────────────────────────────────────────┐
│  LAYER 1 · RAW SOURCES (immutable)                  │
│  Articles · Papers · PDFs · URLs · Conversations    │
│  The system reads from here. Never modifies.        │
└──────────────────────┬──────────────────────────────┘
                       │  ingest / auto-capture
                       ▼
┌─────────────────────────────────────────────────────┐
│  LAYER 2 · WIKI (LLM-written markdown)              │
│  Concepts · Entities · Decisions · Questions        │
│  The LLM writes and maintains this layer.           │
│  You navigate and read (primarily in Obsidian).     │
└──────────────────────┬──────────────────────────────┘
                       │  structural analysis
                       ▼
┌─────────────────────────────────────────────────────┐
│  LAYER 3 · GRAPH (analysis on top of the wiki)      │
│  Typed nodes · Weighted edges · Communities         │
│  God nodes · Surprise connections · Expertise map   │
│  Built FROM the wiki. Adds what Obsidian can't.     │
└─────────────────────────────────────────────────────┘
```

The architecture combines three mechanisms:

| Mechanism | Role | Controlled by |
|-----------|------|---------------|
| **MCP Server** (`brain serve`) | Deterministic I/O: wiki reads/writes, graph operations, cache, search | Java (deterministic logic) |
| **Skills** (`/ingest`, `/query`, `/lint`) | LLM orchestration: extract, synthesize, write | Claude (prompt templates) |
| **Hooks** (`session-start/end`, `pre-compact`) | Automatic capture from Claude Code sessions | Shell scripts |

---

## Features

### `/ingest <url or file>`
Ingests any source (URL, markdown, PDF) into the wiki. The LLM extracts concepts, entities, decisions and relationships, creates new pages or updates existing ones, detects contradictions with prior knowledge, and rebuilds the knowledge graph.

### `/query <question>`
Answers any question by synthesizing the wiki. Uses BFS graph traversal to find relevant context, reads up to 8 relevant pages, and optionally archives the answer back into the wiki as a `question/` page.

### `/lint`
Health check for the wiki. Detects orphan pages, broken wikilinks, asymmetric backlinks (structural — no LLM needed), plus optional semantic checks: duplicate detection, concepts mentioned without a page, and knowledge gap suggestions.

### Automatic session capture
Every Claude Code session is automatically captured and processed. `session-start` injects relevant context via BFS. `session-end` extracts knowledge from the transcript asynchronously. `pre-compact` preserves knowledge before context compaction.

### Knowledge graph analysis
Builds a JGraphT graph from wikilinks. Computes betweenness centrality (god nodes), detects thematic communities (Girvan-Newman clustering), and finds surprise connections — cross-community edges with high confidence that reveal unexpected relationships.

---

## Tech Stack

| Category | Technology |
|----------|-----------|
| **Runtime** | Java 25 (LTS) — Virtual Threads, Structured Concurrency, Records |
| **Framework** | Spring Boot 3.4 |
| **LLM + MCP Server** | Spring AI 1.1 — `ChatClient`, `@Tool`, `AnthropicChatModel`, MCP server stdio |
| **LLM** | Claude Sonnet 4.6 (writing, queries) · Claude Haiku 4.5 (extraction, batch ops) |
| **Graph** | JGraphT 1.5 — betweenness centrality, Girvan-Newman clustering, BFS/DFS |
| **CLI** | Picocli 4.7 — subcommands, dual mode (MCP server + CLI) |
| **Database** | SQLite — graph persistence, SHA-256 cache, FTS5 full-text search (BM25) |
| **Markdown** | flexmark-java 0.64 — wikilink extraction, markdown parsing |
| **PDF** | Apache PDFBox 3.0 |
| **Build** | Gradle 9.4 (Kotlin DSL) + version catalog |

---

## Project Structure

```
brain/
├── brain-core/          # Domain model: records, enums, port interfaces
│   ├── model/           # WikiPage, GraphNode, GraphEdge, GraphAnalysis...
│   ├── port/            # WikiStore, GraphStore, CacheStore (interfaces)
│   └── config/          # BrainConfig, ModelConfig, BrainConfigLoader
│
├── brain-wiki/          # Markdown filesystem operations
│   ├── WikiStoreFs      # CRUD on markdown files with YAML frontmatter
│   ├── FrontmatterParser
│   ├── WikilinkExtractor
│   ├── IndexManager     # Maintains index.md catalog
│   └── LogAppender      # Append-only log.md
│
├── brain-graph/         # Knowledge graph layer
│   ├── GraphBuilder     # Wiki → graph (wikilinks as EXTRACTED edges)
│   ├── GraphAnalyzer    # God nodes, communities, surprise edges
│   ├── GraphTraversal   # BFS/DFS for context building
│   └── GraphStoreSqlite # SQLite persistence
│
├── brain-ai/            # LLM services (Spring AI)
│   ├── WikiWriterService   # ChatClient: write/update wiki pages
│   ├── ExtractorService    # ChatClient: extract entities from sources
│   ├── QueryService        # ChatClient: synthesize answers
│   └── LintSemanticService # ChatClient: semantic health checks
│
├── brain-search/        # Full-text search
│   ├── SearchIndexer    # SQLite FTS5 indexer
│   └── SearchEngine     # BM25 queries with snippets
│
└── brain-server/        # Spring Boot app (MCP server + CLI)
    ├── mcp/             # @Tool beans: WikiTools, GraphTools, SearchTools...
    └── cli/             # Picocli commands: ingest, query, build, lint, context
```

---

## Configuration

Copy `brain.toml` to `~/.brain/brain.toml` and edit:

```toml
[paths]
wiki_root   = "~/brain/wiki"      # Where your wiki lives
raw_sources = "~/brain/raw"       # Raw source files
graph_db    = "~/brain/brain_graph.db"
schema_file = "~/brain/SCHEMA.md" # Controls LLM behavior per domain

[models]
extraction = "claude-haiku-4-5-20251001"   # Fast, cheap — batch ops
wiki_write = "claude-sonnet-4-6"           # Capable — wiki writing
query      = "claude-sonnet-4-6"           # Capable — synthesis
lint       = "claude-haiku-4-5-20251001"   # Fast, cheap — structural checks

[graph]
max_bfs_hops       = 3
max_context_tokens = 2000
god_nodes_top_n    = 10
community_count    = 5
surprise_min_conf  = 0.7

[capture]
enabled            = true
min_session_tokens = 500    # Skip very short sessions
```

### Claude Code integration

Add to `.claude/settings.json`:

```json
{
  "mcpServers": {
    "brain": {
      "command": "brain",
      "args": ["serve"],
      "env": { "ANTHROPIC_API_KEY": "${ANTHROPIC_API_KEY}" }
    }
  },
  "hooks": {
    "UserPromptSubmit": [{ "hooks": [{ "type": "command", "command": "~/.brain/hooks/session-start.sh" }] }],
    "Stop":            [{ "hooks": [{ "type": "command", "command": "~/.brain/hooks/session-end.sh" }] }],
    "PreCompact":      [{ "hooks": [{ "type": "command", "command": "~/.brain/hooks/pre-compact.sh" }] }]
  }
}
```

---

## Getting Started

```bash
# Build
./gradlew build

# Start the MCP server (Claude Code connects automatically)
brain serve

# Or use the CLI directly
brain ingest https://arxiv.org/abs/1706.03762
brain query "What is attention in transformers?"
brain build          # rebuild the knowledge graph
brain lint           # health check
brain context --project $(pwd)   # session context (used by hooks)
```

**Prerequisites:** Java 25, `ANTHROPIC_API_KEY` environment variable.

---

## Estimated Operating Cost

| Operation | Model | Est. cost | Frequency/month | Total |
|-----------|-------|-----------|-----------------|-------|
| Ingest source | Haiku 4.5 | $0.003 | 30 | ~$0.09 |
| Wiki write / page | Sonnet 4.6 | $0.015 | 60 | ~$0.90 |
| Query + synthesis | Sonnet 4.6 | $0.020 | 100 | ~$2.00 |
| Semantic lint | Sonnet 4.6 | $0.040 | 4 | ~$0.16 |
| Session capture | Haiku 4.5 | $0.002 | 50 | ~$0.10 |
| **Total** | | | | **~$3.25/month** |

---

## Implementation Roadmap

- [x] **Phase 1** — Core: wiki CRUD, MCP server, `/ingest`, `/query`
- [ ] **Phase 2** — Graph: JGraphT analysis, `GRAPH_REPORT.md`, session-start context
- [ ] **Phase 3** — Auto-capture: SHA-256 cache, session hooks, PDF support
- [ ] **Phase 4** — Lint + search: FTS5, `/lint` skill, semantic gap detection
- [ ] **Phase 5** — Export + hardening: GraphML/JSON/HTML, cost tracking, GraalVM native

All work is tracked as [GitHub Issues](https://github.com/MarcosLM11/ai-second-brain/issues).

---

## Author

**Marcos** · [@MarcosLM11](https://github.com/MarcosLM11)
