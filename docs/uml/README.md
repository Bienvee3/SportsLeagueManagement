# UML Diagrams — P39 Sports League Management

PlantUML diagrams (`.puml`) documenting **only what is implemented and verified** in source code. Updated with each milestone.

---

## Available Diagrams

| File | Type | Contents |
|---|---|---|
| [`01-domain-classes.puml`](01-domain-classes.puml) | Class Diagram | 8 JPA entities + 5 enums — exact fields, methods, visibility, and relationships |
| [`02-aggregate-chain.puml`](02-aggregate-chain.puml) | Component / Package | Aggregate boundaries, cascade ownership, external references to `Team` |
| [`03-enum-states.puml`](03-enum-states.puml) | State Diagram | Transitions for `SeasonStatus`, `RoundStatus`, `FixtureStatus`, `SeasonTeamStatus` |
| [`04-infrastructure.puml`](04-infrastructure.puml) | Deployment / Component | Profiles dev/test/prod, HikariCP, Flyway, PostgreSQL, H2, migration chain |

---

## How to Render

### Option 1 — IntelliJ IDEA (recommended)
Install the **PlantUML Integration** plugin (`Settings → Plugins → PlantUML Integration`).  
Open any `.puml` file — live preview appears in a side panel.

### Option 2 — VS Code
Install the **PlantUML** extension (`jebbs.plantuml`).  
Open the `.puml` file and press `Alt+D` for live preview.

### Option 3 — Obsidian
Install the community plugin **Obsidian-PlantUML**.  
`.puml` files in this directory render inline as images.

### Option 4 — CLI (no IDE required)
Requires Java + [plantuml.jar](https://plantuml.com/download):
```bash
java -jar plantuml.jar docs/uml/*.puml -o ../uml-png/
```
Outputs PNGs to `docs/uml-png/`.

### Option 5 — Online (no install)
Paste the `.puml` content at [https://www.plantuml.com/plantuml/uml/](https://www.plantuml.com/plantuml/uml/)

---

## Update Convention

A **new numbered `.puml` file** is created for each new layer added:

| Milestone | New File |
|---|---|
| M2 — Repositories + Services | `05-service-layer.puml` |
| M2 — DTOs & Mappers | `06-dto-mapping.puml` |
| M3 — MVC Controllers + REST | `07-web-layer.puml` |
| M3 — Spring Security (roles) | `08-security.puml` |
| M3 — AI Ports & Adapters | `09-ai-hexagonal.puml` |

Domain diagrams (`01`, `02`, `03`) are updated whenever the domain model changes.

---

## Color Legend (01-domain-classes.puml)

| Color | Entity |
|---|---|
| Blue `#4e79a7` | League (Aggregate Root) |
| Green `#59a14f` | Season, Team |
| Orange `#f28e2b` | Round |
| Red `#e15759` | Fixture |
| Yellow `#edc948` | MatchEvent |
| Purple `#b07aa1` | ResultAudit |
| Pink `#ff9da7` | SeasonTeam |
| Cyan `#76b7b2` | Enums |
