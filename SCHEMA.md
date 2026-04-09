# Schema — Second Brain

## Dominio
[Describe el dominio de tu segundo cerebro: personal / research / coding / etc.]

## Idioma
Escribe todas las páginas en español.

## Tipos de nodo activos
- concept: conceptos técnicos o de dominio
- entity: personas, proyectos, organizaciones, herramientas
- decision: decisiones tomadas con su rationale
- question: preguntas con respuesta archivada
- source: resúmenes de fuentes ingestadas

## Estructura de páginas

### Concepto
```yaml
---
title: "[Nombre del concepto]"
type: concept
aliases: []
tags: []
sources: []
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```
# [Título]
[Definición en 2-3 párrafos]

## Related
- [[concepto-relacionado]] — razón del enlace

---

### Entidad
```yaml
---
title: "[Nombre]"
type: entity
aliases: []
tags: []
sources: []
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```
# [Nombre]
[Descripción]

## Related
- [[...]] — razón del enlace

---

### Decisión
```yaml
---
title: "[Decisión tomada]"
type: decision
tags: []
sources: []
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```
# [Decisión]
**Estado**: active | superseded | reverted

## Contexto
[¿Por qué fue necesaria esta decisión?]

## Rationale
[¿Por qué se tomó esta opción y no otras?]

## Consecuencias
[¿Qué implicaciones tiene?]

## Related
- [[...]] — razón del enlace

---

### Pregunta
```yaml
---
title: "[Pregunta]"
type: question
tags: []
sources: []
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```
# [Pregunta]
[Respuesta archivada]

## Related
- [[...]] — razón del enlace

---

### Fuente
```yaml
---
title: "[Título de la fuente]"
type: source
aliases: []
tags: []
url: ""
created: YYYY-MM-DD
updated: YYYY-MM-DD
---
```
# [Título]
[Resumen de la fuente]

## Conceptos clave extraídos
- [[...]]

## Related
- [[...]]
