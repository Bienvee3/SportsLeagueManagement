# Operational AI Agent Instructions — P39 Sports League Management

This document serves as the **single source of technical and operational truth** for any AI agent (OMP, Claude Code, or others) operating within this repository. All rules and guidelines defined here must be strictly adhered to.

---

## 1. Project Context & Identity

- **Course**: Rapid Development of Java Applications Using Programming Frameworks
- **University**: Algebra Bernays University — Academic Year 2026/2027
- **Project**: P39 – Sports League Management
- **Student**: Doalto Cavada Bienvenido
- **Repository**: `https://github.com/Bienvee3/SportsLeagueManagement`
- **Default Branch**: `main`
- **Local Path**: `C:\Users\Bienve\dev\Java\SportsLeagueManagement`

---

## 2. Verified Technology Stack

- **Language & Runtime**: Java 25 LTS (Microsoft Build of OpenJDK)
- **Framework**: Spring Boot 4.1.1
- **Persistence & ORM**: Spring Data JPA over Hibernate 7.4.5.Final
- **Database (Development)**: PostgreSQL 17 (via Docker Compose `postgres:17-alpine`)
- **Database (Testing)**: H2 Database 2.4.240 in-memory (`test` profile, PostgreSQL compatibility mode)
- **Database Migrations**: Flyway (`spring.jpa.hibernate.ddl-auto=validate`)
- **Security**: Spring Security 7 (Form Login, CSRF, BCrypt, 3 distinct roles)
- **Frontend**: Thymeleaf with layout dialect and Thymeleaf Security 6
- **Testing**: JUnit 6 (Jupiter), Mockito, Testcontainers (configured for Milestone 4)
- **Build System**: Maven Wrapper (`mvnw.cmd` on Windows, `mvnw` on POSIX)

---

## 3. Domain Architecture & Code Conventions

### 3.1 Aggregate Chain
$$\text{League} \longrightarrow \text{Season} \longrightarrow \text{Round} \longrightarrow \text{Fixture} \longrightarrow \text{Team}$$
$$\text{Fixture} \longrightarrow \text{MatchEvent} \quad \text{and} \quad \text{Fixture} \longrightarrow \text{ResultAudit}$$

### 3.2 Mandatory Clean Code Standards
1. **Strict Constructor Injection**: Field injection via `@Autowired` on class fields is strictly forbidden. All dependencies must be `final` and injected via constructor.
2. **Rich Domain Model**:
   - Anemic domain models with public indiscriminate setters are prohibited. State transitions must occur through domain methods with explicit business semantics (`addRound(...)`, `recordEvent(...)`).
   - Internal collections must be returned encapsulated using defensive unmodifiable views (`Collections.unmodifiableList(...)`).
3. **Business Key Equality**:
   - Entity `equals` and `hashCode` implementations must be based strictly on immutable business keys (e.g., UUID or unique business natural key), never on database-generated synthetic identifiers (`id`).
4. **Layered Separation of Concerns**:
   - Business and transaction logic resides strictly within domain entities and the application service layer (`@Transactional`).
   - Controllers handle only HTTP requests, Bean Validation validation bindings, and POST-Redirect-GET flows.
5. **Entity Exposure Prohibition**:
   - JPA entities must never be exposed directly through web controllers or REST endpoints. All external input and output must use dedicated DTOs and `ProblemDetail` (RFC 7807) for error payloads.
6. **Immutable Database Schema**:
   - `spring.jpa.hibernate.ddl-auto=validate`. Any alteration to tables, columns, indexes, or constraints must be executed through a new versioned Flyway migration script under `src/main/resources/db/migration/` (e.g., `V3__...sql`).
   - In table `match_events`, the column is named **`match_minute`** (in the Java entity: `private int matchMinute;` with `getMatchMinute()`) to prevent reserved keyword collisions in H2.

---

## 4. Build & Testing Commands

In the Windows environment, always invoke Maven commands via `cmd.exe`:

```powershell
# Run the full test suite
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd test"

# Compile project classes without running tests
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd compile"

# Run the Spring Boot application locally
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd spring-boot:run"

# Start local PostgreSQL container (when Docker Desktop is installed)
docker compose up -d
```

> **Known Notice**: The Mockito JVM self-attaching warning on Java 25 is expected and benign. It will be formally addressed in Milestone 4 by adding the `-javaagent` argument to the `maven-surefire-plugin` configuration.

---

## 5. Security & Secret Policies

- **Zero Secrets**: Committing API keys, database credentials, authentication tokens, or private environment variables into the repository is strictly forbidden.
- **Sensitive Configuration**: All sensitive values must be injected via system environment variables or untracked local profile files.
- **Security Roles**:
  - `ROLE_ADMIN`: Full administrative access (leagues, seasons, teams, users).
  - `ROLE_OFFICIAL`: Match score entry, match events recording, and audited result corrections.
  - `ROLE_VIEWER`: Read-only access to standings, fixtures, and AI match chronicles.

---

## 6. Git & Commit Guidelines

1. **Atomic Commits**: Each commit must represent exactly one logical unit of work.
2. **Conventional Commits Specification**:
   ```text
   <type>(<scope>): <concise description in imperative present tense>
   ```
   *Types*: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`.  
   *Scopes*: `domain`, `db`, `repo`, `service`, `security`, `web`, `ai`, `infra`, `test`.
3. **Always Green**: Never propose a commit without first verifying `BUILD SUCCESS` from `mvnw.cmd test`.
4. **STRICT PUSH CONTROL**: **Pushing (`git push`) without explicit prior authorization from the user is STRICTLY FORBIDDEN**. The agent must commit locally, verify tests, and await the user's consent before publishing.

---

## 7. Standard Step-by-Step Task Procedure

1. **Read**: Inspect `AGENTS.md` and `docs/CURRENT_STATUS.md`.
2. **Plan**: Define clear target scope and acceptance criteria.
3. **Inspect**: Read relevant codebase files before making modifications.
4. **Implement**: Apply focused, surgical changes adhering to conventions.
5. **Verify**: Run `cmd.exe /c "mvnw.cmd test"` and confirm all tests pass.
6. **Update Status**: Reflect progress and test results in `docs/CURRENT_STATUS.md`.
7. **Propose Commit**: Summarize modified files, test outputs, and propose commit message.

---

## 8. Updating Project Status

Upon completing any task or work session, the agent **must update** `docs/CURRENT_STATUS.md`, providing:
- Last completed milestone step.
- Exact verification and test results.
- Immediate pending tasks.
- Known issues or technical constraints.

---

## 9. Visual Architecture & UML Documentation Directive

- **Mandatory Trigger**: When requested by the user, the agent must produce detailed architectural documentation in `docs/` (compatible with Obsidian Markdown notes).
- **Required Elements**:
  - **Application Distribution**: Clear breakdown of MVC (Model-View-Controller) layers, DTO mapping, and Service transaction boundaries.
  - **Class & Aggregate Diagrams**: UML class diagrams (via Mermaid `classDiagram`) showing entities, business keys, associations, and methods.
  - **Database Schema / ERD**: Entity-relationship diagrams (via Mermaid `erDiagram`) mapping tables, primary keys, foreign keys, and constraints.
  - **Sequence / Flow Diagrams**: Visual flows for core business operations (Round-Robin scheduling, dynamic tie-break standings calculation, and audited result correction).
