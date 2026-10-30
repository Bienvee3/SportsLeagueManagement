# UML Diagrams — P39 Sports League Management

Diagramas en formato **PlantUML** (`.puml`). Documentan **exclusivamente lo implementado y verificado** en código fuente. Se actualizarán con cada milestone.

---

## 📐 Diagramas disponibles

| Archivo | Tipo | Contenido |
|---|---|---|
| [`01-domain-classes.puml`](01-domain-classes.puml) | Class Diagram | 8 entidades JPA + 5 enums con campos, métodos, visibilidad y relaciones exactas |
| [`02-aggregate-chain.puml`](02-aggregate-chain.puml) | Component / Package | Cadena de agregados, fronteras de cascade, ownership, referencias externas a Team |
| [`03-enum-states.puml`](03-enum-states.puml) | State Diagram | Transiciones de SeasonStatus, RoundStatus, FixtureStatus, SeasonTeamStatus |
| [`04-infrastructure.puml`](04-infrastructure.puml) | Deployment / Component | Perfiles dev/test/prod, HikariCP, Flyway, PostgreSQL, H2, cadena de migraciones |

---

## 🖥️ Cómo renderizar los diagramas

### Opción 1 — IntelliJ IDEA (recomendada)
Instala el plugin **PlantUML Integration** (`Settings → Plugins → PlantUML Integration`).  
Abre cualquier `.puml` y el preview se muestra en un panel lateral.

### Opción 2 — VS Code
Instala la extensión **PlantUML** (`jebbs.plantuml`).  
Abre el `.puml` y pulsa `Alt+D` para el preview en tiempo real.

### Opción 3 — Obsidian
Instala el plugin de la comunidad **Obsidian-PlantUML**.  
Los `.puml` de este directorio se renderizan inline como imágenes.

### Opción 4 — CLI (sin IDE)
Requiere Java + [plantuml.jar](https://plantuml.com/download):
```bash
java -jar plantuml.jar docs/uml/*.puml -o ../uml-png/
```
Genera PNGs en `docs/uml-png/`.

### Opción 5 — Online (sin instalar nada)
Pega el contenido del `.puml` en [https://www.plantuml.com/plantuml/uml/](https://www.plantuml.com/plantuml/uml/)

---

## 📌 Convención de actualización

Cuando se añada una nueva capa (Repositorios, Servicios, Controladores, Security, AI), se creará un **nuevo archivo `.puml` numerado**:

| Milestone | Archivo nuevo |
|---|---|
| M2 — Repositorios + Servicios | `05-service-layer.puml` |
| M2 — DTOs y Mappers | `06-dto-mapping.puml` |
| M3 — Controladores MVC + REST | `07-web-layer.puml` |
| M3 — Spring Security (roles) | `08-security.puml` |
| M3 — AI Ports & Adapters | `09-ai-hexagonal.puml` |

Los diagramas de dominio (`01`, `02`, `03`) se actualizarán si cambia el modelo.

---

## 🎨 Leyenda de colores (01-domain-classes.puml)

| Color | Significado |
|---|---|
| 🟦 Azul `#4e79a7` | League (Aggregate Root) |
| 🟩 Verde `#59a14f` | Season, Team, SeasonStatus |
| 🟧 Naranja `#f28e2b` | Round, RoundStatus |
| 🟥 Rojo `#e15759` | Fixture, FixtureStatus |
| 🟨 Amarillo `#edc948` | MatchEvent |
| 🟪 Morado `#b07aa1` | ResultAudit |
| 🩷 Rosa `#ff9da7` | SeasonTeam |
| 🩵 Cyan `#76b7b2` | Enums |
