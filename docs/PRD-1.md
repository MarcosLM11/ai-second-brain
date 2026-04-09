# PRD-1: Second Brain — Requisitos de Producto

> Documento de requisitos de producto. Basado en PLAN-0 (visión) y PLAN-1 (diseño técnico).  
> Lenguaje de implementación: Java 25. Versión del producto: 1.0.

---

## 1. Problema y visión

### Problema

El conocimiento que una persona genera, consume y discute se pierde o queda fragmentado. Las wikis personales se abandonan porque el mantenimiento crece más rápido que el valor. Los sistemas RAG (NotebookLM, ChatGPT uploads) redescubren el conocimiento desde cero en cada consulta — no acumulan nada.

### Visión

Un **segundo cerebro personal** donde el conocimiento se organiza automáticamente, se mantiene actualizado, se interconecta y se vuelve más valioso con el tiempo. El LLM hace el mantenimiento (escribir, enlazar, actualizar, detectar contradicciones). El usuario aporta fuentes, hace preguntas y navega resultados en Obsidian.

### Diferenciación clave

| Sistema | Limitación |
|---------|-----------|
| NotebookLM / ChatGPT uploads | RAG puro: redescubre desde cero en cada consulta, no acumula |
| claude-memory-compiler | Solo sesiones de Claude Code; solo dominio de software |
| graphify | Corpus estático; no captura conversaciones ni evoluciona |
| **Second Brain** | Captura automática + ingesta manual + wiki vivo + análisis en grafo |

---

## 2. Usuarios objetivo

**Usuario primario**: cualquier persona que acumule conocimiento en un dominio y quiera:
- Recuperarlo sin esfuerzo semanas o meses después
- Ver conexiones no obvias entre conceptos
- Saber qué sabe mucho vs. qué sabe poco

**Dominios de uso**:
- Personal: metas, salud, psicología, mejora personal
- Research: papers, artículos, informes — tesis que evoluciona
- Lectura: procesar libros capítulo a capítulo
- Trabajo/equipo: reuniones, Slack, documentos de proyecto
- Coding: decisiones técnicas, patrones, gotchas entre sesiones con IA

---

## 3. Arquitectura de tres capas (referencia de diseño)

```
CAPA 1 · RAW SOURCES (inmutables)
  Conversaciones · Artículos · Papers · PDFs · URLs
  El sistema lee desde aquí. Nunca modifica estas fuentes.

CAPA 2 · WIKI (LLM-written markdown)
  Conceptos · Entidades · Decisiones · Preguntas
  El LLM escribe y mantiene. El usuario navega en Obsidian.

CAPA 3 · GRAPH (análisis sobre el wiki)
  Nodos · Aristas · Comunidades · God nodes · Expertise
  Se construye desde el wiki. Añade análisis estructural.
```

---

## 4. Requisitos funcionales

### 4.1 Ingesta de fuentes (RF-INGEST)

| ID | Requisito |
|----|-----------|
| RF-INGEST-01 | El sistema permite ingestar URLs (artículos web, papers) vía comando `/ingest <url>` |
| RF-INGEST-02 | El sistema permite ingestar ficheros locales: `.md`, `.txt`, `.pdf` |
| RF-INGEST-03 | Antes de procesar, el sistema calcula un hash SHA-256 de la fuente y verifica si ya fue procesada (caché) |
| RF-INGEST-04 | Si la fuente ya está en caché, el sistema informa al usuario y detiene el proceso |
| RF-INGEST-05 | El sistema extrae entidades, conceptos, decisiones y relaciones clave de la fuente |
| RF-INGEST-06 | El sistema detecta qué páginas wiki existentes están relacionadas con el contenido nuevo |
| RF-INGEST-07 | El sistema crea páginas wiki nuevas para conceptos/entidades no existentes |
| RF-INGEST-08 | El sistema actualiza páginas wiki existentes cuando la fuente aporta información nueva sobre ellas |
| RF-INGEST-09 | Cuando nueva información contradice contenido existente, el sistema marca la contradicción en ambas páginas |
| RF-INGEST-10 | El sistema actualiza `index.md` al finalizar cada ingesta |
| RF-INGEST-11 | El sistema registra cada ingesta en `log.md` con fecha, tipo y título de la fuente |
| RF-INGEST-12 | El sistema reconstruye el grafo incrementalmente tras cada ingesta |

### 4.2 Escritura y mantenimiento del wiki (RF-WIKI)

| ID | Requisito |
|----|-----------|
| RF-WIKI-01 | Todas las páginas siguen el formato definido en `SCHEMA.md` (frontmatter YAML + cuerpo markdown) |
| RF-WIKI-02 | El Schema es configurable por dominio sin recompilar el sistema |
| RF-WIKI-03 | Las páginas contienen wikilinks `[[...]]` a conceptos relacionados existentes |
| RF-WIKI-04 | Las afirmaciones inferidas (no extraídas directamente de la fuente) se marcan con `*[inferred]*` |
| RF-WIKI-05 | El wiki mantiene un `index.md` con catálogo de todas las páginas y sus resúmenes |
| RF-WIKI-06 | El wiki mantiene un `log.md` de todas las operaciones (ingest, query archivado, lint) |
| RF-WIKI-07 | Los tipos de página soportados son: `concept`, `entity`, `decision`, `question`, `source` |
| RF-WIKI-08 | El Schema puede añadir tipos de nodo adicionales específicos del dominio |
| RF-WIKI-09 | Los ficheros del wiki son markdown estándar, legibles y navegables en Obsidian sin plugins |

### 4.3 Captura automática de sesiones (RF-CAPTURE)

| ID | Requisito |
|----|-----------|
| RF-CAPTURE-01 | Al inicio de cada sesión de Claude Code, el sistema inyecta contexto relevante del brain en el prompt del sistema |
| RF-CAPTURE-02 | El contexto inyectado se genera con BFS desde el nodo del proyecto activo (máx. 2000 tokens) |
| RF-CAPTURE-03 | Al finalizar una sesión, el sistema extrae conocimiento de la transcripción y lo ingesta automáticamente |
| RF-CAPTURE-04 | La extracción post-sesión es asíncrona (no bloquea el cierre de sesión) |
| RF-CAPTURE-05 | El sistema no captura sesiones por debajo de un umbral de tokens configurable (por defecto: 500) |
| RF-CAPTURE-06 | Antes de compactar el contexto (pre-compact), el sistema guarda el conocimiento generado hasta ese punto |

### 4.4 Consulta y síntesis (RF-QUERY)

| ID | Requisito |
|----|-----------|
| RF-QUERY-01 | El usuario puede hacer preguntas en lenguaje natural vía comando `/query <pregunta>` |
| RF-QUERY-02 | El sistema identifica las páginas más relevantes para la pregunta usando el índice y el grafo |
| RF-QUERY-03 | El sistema lee hasta 8 páginas relevantes y sintetiza una respuesta con citas wikilink |
| RF-QUERY-04 | Al finalizar, el sistema ofrece al usuario archivar la respuesta en el wiki como página `question/` |
| RF-QUERY-05 | Las respuestas archivadas actualizan los wikilinks en las páginas citadas |
| RF-QUERY-06 | Cuando el wiki supera 100 páginas, `/query` utiliza búsqueda FTS5 para pre-filtrar páginas relevantes |

### 4.5 Análisis de grafo (RF-GRAPH)

| ID | Requisito |
|----|-----------|
| RF-GRAPH-01 | El sistema construye un grafo de conocimiento desde los wikilinks del wiki |
| RF-GRAPH-02 | El grafo soporta aristas tipadas: `LINKS_TO`, `EXPLAINS`, `DEPENDS_ON`, `CONTRADICTS`, `EVOLVED_FROM`, `USED_IN`, `CAUSED_BY`, `ANSWERED_BY`, `CITED_IN`, `RELATED_TO` |
| RF-GRAPH-03 | Cada arista tiene un origen (`EXTRACTED`, `INFERRED`, `AMBIGUOUS`) y un nivel de confianza (0.0–1.0) |
| RF-GRAPH-04 | El sistema identifica "god nodes": páginas con alta centralidad de intermediación (betweenness centrality) |
| RF-GRAPH-05 | El sistema detecta comunidades temáticas mediante clustering topológico (Girvan-Newman) |
| RF-GRAPH-06 | El sistema detecta "conexiones sorpresa": aristas cross-community con alta confianza (≥ 0.7) |
| RF-GRAPH-07 | El sistema genera un `GRAPH_REPORT.md` con los resultados del análisis |
| RF-GRAPH-08 | El build del grafo es incremental: solo reprocesa páginas modificadas desde el último build |
| RF-GRAPH-09 | El grafo persiste en SQLite para consultas rápidas sin reconstrucción completa |

### 4.6 Búsqueda (RF-SEARCH)

| ID | Requisito |
|----|-----------|
| RF-SEARCH-01 | El sistema indexa el wiki completo con búsqueda de texto completo (FTS5) |
| RF-SEARCH-02 | La búsqueda devuelve resultados ordenados por relevancia BM25 con snippets |
| RF-SEARCH-03 | El índice de búsqueda se actualiza incrementalmente al escribir páginas nuevas o actualizadas |
| RF-SEARCH-04 | La búsqueda no requiere LLM — es operación determinista local |

### 4.7 Lint y salud del wiki (RF-LINT)

| ID | Requisito |
|----|-----------|
| RF-LINT-01 | El sistema detecta páginas huérfanas (sin referencias entrantes) |
| RF-LINT-02 | El sistema detecta wikilinks rotos (referencia a página inexistente) |
| RF-LINT-03 | El sistema detecta backlinks asimétricos (A enlaza B pero B no enlaza A cuando debería) |
| RF-LINT-04 | El sistema detecta posibles páginas duplicadas por similitud de título |
| RF-LINT-05 | El sistema identifica conceptos mencionados en el wiki sin página propia |
| RF-LINT-06 | El sistema sugiere 3–5 preguntas para llenar gaps de conocimiento detectados |
| RF-LINT-07 | El lint estructural (RF-LINT-01 a 03) no requiere LLM |
| RF-LINT-08 | El sistema genera un `HEALTH_REPORT.md` con los resultados del lint |

### 4.8 Exportación (RF-EXPORT)

| ID | Requisito |
|----|-----------|
| RF-EXPORT-01 | El sistema exporta el grafo a formato GraphML |
| RF-EXPORT-02 | El sistema exporta el grafo a formato JSON |
| RF-EXPORT-03 | El sistema exporta el wiki a HTML con visualización del grafo (D3.js) |

---

## 5. Requisitos no funcionales

### 5.1 Rendimiento (RNF-PERF)

| ID | Requisito |
|----|-----------|
| RNF-PERF-01 | El hook `session-start` completa en < 3 segundos (el contexto inyectado no debe ralentizar el inicio de sesión) |
| RNF-PERF-02 | La operación `graph_build` incremental completa en < 10 segundos para wikis de hasta 500 páginas |
| RNF-PERF-03 | La búsqueda FTS5 devuelve resultados en < 500ms para wikis de hasta 10.000 páginas |
| RNF-PERF-04 | El extractor de sesión opera en background y no bloquea el cierre del hook `session-end` |
| RNF-PERF-05 | En Fase 5, el CLI arranca en < 100ms mediante GraalVM native image |

### 5.2 Coste operacional (RNF-COST)

| ID | Requisito |
|----|-----------|
| RNF-COST-01 | El coste mensual estimado para uso normal (30 ingestas, 100 queries, 50 capturas, 4 lints) no supera $5 |
| RNF-COST-02 | Las operaciones batch (extracción, lint básico) usan Claude Haiku para minimizar coste |
| RNF-COST-03 | Las operaciones de escritura y síntesis usan Claude Sonnet |
| RNF-COST-04 | El sistema proporciona tracking de coste acumulado (tokens × precio por modelo) |

### 5.3 Seguridad (RNF-SEC)

| ID | Requisito |
|----|-----------|
| RNF-SEC-01 | El sistema previene path traversal en operaciones `wiki_read`/`wiki_write` validando que el path resuelto está bajo `wiki_root` |
| RNF-SEC-02 | El sistema previene SSRF en ingesta de URLs: solo esquema `https`, sin IPs privadas RFC-1918/loopback, timeout 10s |
| RNF-SEC-03 | Las páginas wiki se pasan al LLM como contexto de usuario, no como instrucciones de sistema (mitiga prompt injection) |
| RNF-SEC-04 | La API key de Anthropic se lee exclusivamente desde variable de entorno; nunca aparece en logs ni en respuestas de tools |
| RNF-SEC-05 | Los paths de base de datos y configuración se leen de `brain.toml`, no de inputs externos al servidor |
| RNF-SEC-06 | La exportación HTML escapa correctamente el contenido (previene XSS) |

### 5.4 Portabilidad y despliegue (RNF-DEPLOY)

| ID | Requisito |
|----|-----------|
| RNF-DEPLOY-01 | El sistema funciona offline salvo las llamadas a la API de Anthropic |
| RNF-DEPLOY-02 | El sistema no depende de ningún proveedor cloud específico |
| RNF-DEPLOY-03 | La base de datos es SQLite local; no requiere servidor de base de datos externo |
| RNF-DEPLOY-04 | El wiki es markdown puro en filesystem; portable a cualquier herramienta compatible (Obsidian, VS Code, etc.) |
| RNF-DEPLOY-05 | El sistema es configurable mediante `brain.toml` sin recompilar |
| RNF-DEPLOY-06 | El usuario puede tener múltiples wikis independientes con distintos `brain.toml` vía `--config` |

### 5.5 Mantenibilidad (RNF-MAINT)

| ID | Requisito |
|----|-----------|
| RNF-MAINT-01 | El schema del wiki (tipos de página, idioma, estructura) es configurable en `SCHEMA.md` sin recompilar |
| RNF-MAINT-02 | Los Skills de Claude Code (`.claude/commands/*.md`) son editables sin recompilar |
| RNF-MAINT-03 | Los unit tests no requieren API key de Anthropic ni conexión de red |
| RNF-MAINT-04 | El proyecto está organizado en módulos Gradle independientes con responsabilidades claras |

---

## 6. Inventario de herramientas MCP (referencia de contrato)

El servidor MCP expone las siguientes 17 tools a Claude Code:

| Tool | Descripción |
|------|-------------|
| `wiki_read` | Lee una página del wiki (frontmatter + markdown) |
| `wiki_write` | Escribe o actualiza una página del wiki |
| `wiki_list` | Lista páginas filtrando por tipo o patrón glob |
| `wiki_index_read` | Lee el catálogo `index.md` |
| `log_append` | Añade entrada al `log.md` |
| `graph_build` | Construye/actualiza el grafo desde el wiki |
| `graph_query` | BFS desde un nodo, devuelve subgrafo JSON |
| `graph_analyze` | God nodes, comunidades, conexiones sorpresa |
| `graph_report_read` | Lee `GRAPH_REPORT.md` |
| `cache_check` | Verifica si una fuente ya fue procesada (por hash) |
| `cache_set` | Registra una fuente como procesada |
| `source_hash` | Calcula SHA-256 de un fichero o URL |
| `lint_structural` | Huérfanos, links rotos, backlinks asimétricos |
| `search` | Búsqueda BM25 sobre el wiki |
| `search_index_update` | Actualiza el índice FTS5 |
| `schema_read` | Lee el Schema activo |
| `config_read` | Lee la configuración del brain (sin secretos) |

---

## 7. Skills de Claude Code

Tres comandos disponibles para el usuario:

| Skill | Comando | Descripción |
|-------|---------|-------------|
| Ingest | `/ingest <fuente>` | Ingesta una URL o fichero en el wiki |
| Query | `/query <pregunta>` | Responde una pregunta sintetizando el wiki |
| Lint | `/lint` | Chequeo de salud estructural y semántico |

---

## 8. Criterios de aceptación por fase

### Fase 1 — Núcleo funcional

- [ ] `/ingest` con una URL real produce páginas markdown válidas con frontmatter YAML
- [ ] Las páginas generadas contienen wikilinks a conceptos existentes
- [ ] `/query` sobre el wiki generado devuelve respuesta con citas
- [ ] Ingestar 5 artículos manualmente: wiki crece coherentemente sin páginas duplicadas

### Fase 2 — Capa de grafo

- [ ] `brain build` sobre wiki con 20+ páginas genera `GRAPH_REPORT.md` con god nodes y comunidades
- [ ] `graph_query` desde cualquier nodo devuelve subgrafo JSON válido
- [ ] `session-start.sh` inyecta contexto BFS relevante al proyecto activo

### Fase 3 — Captura automática

- [ ] Transcripción de sesión real genera páginas wiki en background sin bloquear el hook
- [ ] `cache_check` evita reprocesar fuentes ya ingestadas
- [ ] Una semana de uso real: sesiones capturadas automáticamente sin intervención del usuario

### Fase 4 — Lint y búsqueda

- [ ] `lint_structural` detecta links rotos y huérfanos reales en wiki activo
- [ ] `search` devuelve resultados relevantes con snippets en < 500ms
- [ ] `/lint` semanal detecta al menos un gap o duplicado real

### Fase 5 — Exportación y hardening

- [ ] Exportación HTML renderiza el grafo navegable con D3.js
- [ ] CLI arranca en < 100ms con GraalVM native image
- [ ] Tests de integración end-to-end pasan con `ANTHROPIC_API_KEY` real

---

## 9. Coste operacional estimado

| Operación | Modelo | Coste/unidad | Frecuencia/mes | Total |
|-----------|--------|-------------|----------------|-------|
| Ingest fuente (~3K in + ~2K out tokens) | Haiku 4.5 | $0.003 | 30 | ~$0.09 |
| Wiki write por página (~500 out tokens) | Sonnet 4.6 | $0.015 | 60 | ~$0.90 |
| Query con síntesis (~4K tokens total) | Sonnet 4.6 | $0.020 | 100 | ~$2.00 |
| Lint semántico (~8K tokens) | Sonnet 4.6 | $0.040 | 4 | ~$0.16 |
| Captura de sesión (~2K tokens) | Haiku 4.5 | $0.002 | 50 | ~$0.10 |
| **Total estimado** | | | | **~$3.25/mes** |

---

## 10. Fuera de alcance (v1.0)

- **Embeddings/semántica vectorial**: la deduplicación semántica usa naming conventions + lint manual; embeddings se evalúan en v2
- **Clustering Leiden/Louvain**: se usa Girvan-Newman (JGraphT built-in); algoritmos más avanzados se evalúan en Fase 4
- **Multi-usuario / compartido**: el sistema es personal; un wiki por `brain.toml`
- **Interfaz web propia**: el usuario navega en Obsidian; no hay UI propia en v1
- **Ingesta de imágenes/audio**: solo texto (markdown, PDF, HTML)
- **Sincronización en la nube**: el wiki vive en filesystem local; sincronización vía herramientas externas (iCloud, Git)

---

## 11. Dependencias técnicas principales

| Categoría | Tecnología | Versión |
|-----------|-----------|---------|
| Runtime | Java 25 (LTS) | 25+ |
| Framework | Spring Boot | 3.4.x |
| LLM + MCP Server | Spring AI | 1.0.x |
| Modelo LLM | Claude Sonnet 4.6 / Haiku 4.5 | — |
| Grafo | JGraphT | 1.5.2 |
| CLI | Picocli | 4.7.6 |
| Base de datos | SQLite (xerial/sqlite-jdbc) | 3.47.x |
| Markdown parsing | flexmark-java | 0.64.x |
| PDF parsing | Apache PDFBox | 3.0.x |
| Build | Gradle (Kotlin DSL) | 8.12+ |

---

## 12. Decisiones de diseño tomadas

| Decisión | Opción elegida | Razón |
|----------|---------------|-------|
| Protocolo de integración con Claude Code | MCP Server (stdio) + Skills | Separación limpia: Java hace lo determinista, Claude hace lo semántico |
| Persistencia del grafo | SQLite local | Sin dependencias externas; FTS5 integrado; portable |
| Schema del wiki | `SCHEMA.md` en markdown | Legible directamente por el LLM sin parseo especial |
| Distribución del CLI | Fat JAR (dev) → GraalVM native (Fase 5) | Arranque < 100ms necesario para hooks; nativo diferido a Fase 5 |
| Formato de configuración | `brain.toml` | Más legible que YAML para configuración del sistema; idiomático en herramientas CLI |
| Multi-wiki | Un `brain.toml` por wiki | Simplicidad y aislamiento; el usuario gestiona múltiples wikis vía `--config` |

---

## 13. Riesgos

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|-------------|---------|-----------|
| Calidad de extracción LLM inconsistente | Media | Alto | Structured output vía Spring AI; prompt con Schema estricto |
| Coste real supera estimado | Baja | Medio | Tracking de tokens por operación desde Fase 3 |
| Wiki crece con páginas duplicadas | Media | Medio | `lint_structural` periódico; naming conventions en Schema |
| Arranque de Spring Boot lento para hooks | Alta | Alto | GraalVM native image en Fase 5; fat JAR tolerable en desarrollo |
| Transcripción de sesión no disponible en hook | Media | Alto | PLAN-1 §8 documenta mecanismo via `$CLAUDE_SESSION_TRANSCRIPT`; validar en Fase 3 |
