# System Architecture — Sports League Management

This document outlines the real technical architecture of **Sports League Management**, clearly distinguishing what is already implemented and verified in code from architectural decisions and layers planned for future milestones.

---

## 1. Overall Architectural Overview

The system adheres to a **Clean Layered Architecture** coupled with **Ports and Adapters (Hexagonal Architecture)** for external integrations (specifically the AI module).

```
   ┌──────────────────────────────────────────────────────────────┐
   │                    Web / Presentation Layer                  │
   │  ┌─────────────────────────────┐ ┌────────────────────────┐  │
   │  │ Thymeleaf Views (HTML5/CSS3)│ │ REST API (OpenAPI 3.x) │  │
   │  └──────────────┬──────────────┘ └───────────┬────────────┘  │
   └─────────────────┼────────────────────────────┼───────────────┘
                     ▼                            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │                 Application / Service Layer                  │
   │  ┌─────────────────────────────┐ ┌────────────────────────┐  │
   │  │ Application Services (@Tx)  │ │ DTOs / Mappers / Valid.│  │
   │  └──────────────┬──────────────┘ └────────────────────────┘  │
   └─────────────────┼────────────────────────────────────────────┘
                     ▼
   ┌──────────────────────────────────────────────────────────────┐
   │                     Domain Layer (Core)                      │
   │  ┌────────────────────────────────────────────────────────┐  │
   │  │ Rich Entities, Aggregates, Business Invariants, Enums  │  │
   │  │ AI Port Interfaces (Inbound/Outbound)                  │  │
   │  └──────────────────────────────┬─────────────────────────┘  │
   └─────────────────────────────────┼────────────────────────────┘
                     ▼               ▼
   ┌─────────────────────────────────┼────────────────────────────┐
   │                    Infrastructure Layer                      │
   │  ┌──────────────────────────────┴─┐ ┌─────────────────────┐  │
   │  │ Spring Data JPA / Hibernate 7  │ │ AI External Adapter │  │
   │  │ PostgreSQL 17 + Flyway DDL     │ │ (Spring AI / HTTP)  │  │
   │  └────────────────────────────────┘ └─────────────────────┘  │
   └──────────────────────────────────────────────────────────────┘
```

---

## 2. Real Existing Implementation vs. Future Scope

### 2.1 Domain Layer (`com.league.sportsleague.domain`) — **EXISTING (100%)**
The rich domain model is fully implemented and verified via unit tests (`DomainModelTest`):
- **`League`** (Aggregate Root): Root entity representing a sports league; manages its collection of seasons.
- **`Season`**: Represents an active or archived competition cycle (`SeasonStatus`: `DRAFT`, `ACTIVE`, `COMPLETED`, `ARCHIVED`).
- **`Team`**: Represents a participating club with business key equality and invariant validation.
- **`SeasonTeam`**: Many-to-Many association entity carrying registration metadata (`registeredAt`, `SeasonTeamStatus`).
- **`Round`**: Competition matchday grouped under a season (`RoundStatus`).
- **`Fixture`**: Scheduled or played match between home and away teams, enforcing team distinctness invariants, scores, and statuses (`SCHEDULED`, `IN_PROGRESS`, `FINISHED`, `POSTPONED`, `CANCELLED`).
- **`MatchEvent`**: In-game event (goal, card, substitution) with persistence column `match_minute`.
- **`ResultAudit`**: Immutable audit record tracking retroactive score adjustments and reasons (Advanced Requirement 3).

### 2.2 Persistence & Database Layer — **PARTIALLY EXISTING**
- **Existing**:
  - Flyway DDL migration `V1__init_schema.sql` with foreign keys, composite constraints, and validation checks.
  - Flyway seed migration `V2__seed_initial_data.sql`.
  - JPA entity mappings over Hibernate 7.4.5 with `FetchType.LAZY` and defensive unmodifiable collection getters.
  - HikariCP pool configuration and in-memory H2 compatibility test profile.
- **Pending (Milestone 2 - Step 2.1)**:
  - Spring Data JPA Repository interfaces (`LeagueRepository`, `FixtureRepository`, etc.) with explicit anti-N+1 `@Query` fetch joins.

### 2.3 Application & Service Layer — **FUTURE (Milestone 2)**
- **Planned**:
  - Application services with strict constructor injection: `LeagueService`, `TeamService`, `SeasonService`, `FixtureService`.
  - Mathematical Round-Robin fixture generation algorithm with home/away balance (Advanced Requirement 1).
  - Dynamic standings calculation engine supporting 5 strict tie-break rules in cascade (Advanced Requirement 2).
  - Result correction service with audit logging and retroactive standings recomputation (Advanced Requirement 3).
  - Dedicated Request/Response DTOs preventing JPA entity exposure.

### 2.4 Web & Presentation Layer — **FUTURE (Milestones 2 & 3)**
- **Planned**:
  - Spring Web MVC controllers with Thymeleaf views, layout dialect, Bean Validation, and POST-Redirect-GET flow.
  - RESTful API controllers with springdoc-openapi 3.x documentation.
  - Standardized error handling using `ProblemDetail` (RFC 7807).

### 2.5 AI Integration Module — **FUTURE (Milestone 3)**
- **Architecture**: Hexagonal Ports & Adapters.
- **Domain Port**: Clean Java interface defining match chronicle generation contracts based on structured fixture and event data.
- **Infrastructure Adapter**: External LLM adapter (Gemini or OpenAI via Spring AI / HTTP) adhering to fact-strict prompt engineering (zero hallucinations, verbatim score validation).
- **Bilingual Output**: Generates chronicles in both English and Croatian.
- **Testing Strategy**: Offline Stub adapter implementation allowing deterministic CI/CD runs without consuming external API quota.

### 2.6 Security Layer — **PARTIALLY EXISTING**
- **Existing**: Spring Security 7 and Thymeleaf Security dependencies active in `pom.xml`. Test runtime authentication setup.
- **Pending (Milestone 3)**: Formal `SecurityFilterChain` bean configuring Form Login, CSRF protection, BCrypt password hashing, and 3 distinct authorization roles (`ROLE_ADMIN`, `ROLE_OFFICIAL`, `ROLE_VIEWER`) verified with 403 Forbidden authorization tests.

---

## 3. Core Architectural Decisions

1. **Relational Persistence over Document Store**: Justified in [ADR 001](adr/001-persistence-model.md) to guarantee ACID integrity during complex standings recalculations and audit trail logging.
2. **Flyway Validate Mode**: Schemas managed exclusively as versioned code; runtime DDL auto-generation is prohibited in production and grading environments.
3. **Constructor-Only Injection**: Guarantees thread-safe immutability, prevents circular dependencies, and simplifies framework-free unit testing.
4. **Hexagonal Ports & Adapters for AI**: The core domain remains agnostic of external AI SDKs and HTTP APIs, facilitating seamless test mocking and provider substitution.
