# Architecture & Design Decisions Log

This directory centralizes the record of verified technical decisions for the **Sports League Management** project. All decisions cataloged here are backed by actual code, configurations, or pre-existing architectural records.

---

## 📋 Verified Technical Decisions Index

| ID | Date | Title | Status | Backing Evidence / Document |
| :---: | :---: | :--- | :---: | :--- |
| **DEC-001** | 2026-10-13 | **Relational Persistence with PostgreSQL 17, JPA, and Flyway** | Accepted | [ADR 001](../adr/001-persistence-model.md) |
| **DEC-002** | 2026-10-14 | **Strict Constructor Injection Policy** | Accepted | [Development Guidelines](../development-guidelines.md) § 2.2 · [AGENTS.md](../../AGENTS.md) |
| **DEC-003** | 2026-10-14 | **Rich Domain Model: Collection Immutability & Business Key Equality** | Accepted | Entity source code in `com.league.sportsleague.domain` |
| **DEC-004** | 2026-10-15 | **Column Naming: `match_minute` for H2 Test Compatibility** | Accepted | Commit `193bc6d` · `V1__init_schema.sql` · `MatchEvent.java` |
| **DEC-005** | 2026-10-21 | **Shared AI Agent Memory Architecture (`AGENTS.md` + `CLAUDE.md`)** | Accepted | `AGENTS.md` (master) · `CLAUDE.md` (reference) |

---

## 🔍 Key Decision Summaries

### DEC-001: Relational Persistence with PostgreSQL, Hibernate 7, and Flyway
- **Context**: Multi-tier domain requiring transactional integrity for retroactive standings recomputation and audit logging.
- **Decision**: PostgreSQL 17 as relational engine, Hibernate 7 in `ddl-auto=validate` mode, deterministic schema management via versioned Flyway scripts.
- **Full Details**: See [`docs/adr/001-persistence-model.md`](../adr/001-persistence-model.md).

### DEC-002: Exclusive Constructor Injection
- **Context**: Preventing hidden circular dependencies, enabling clean unit testing without Spring context overhead, and satisfying course Learning Outcome 1 (LO1).
- **Decision**: Field injection with `@Autowired` is prohibited across all classes. Dependencies must be declared `final` and injected via public constructors.

### DEC-003: Rich Domain Model with Defensive Copying
- **Context**: Protecting aggregate boundaries against unauthorized mutations from outside the aggregate root.
- **Decision**:
  - Collection getters return immutable views (`Collections.unmodifiableList(...)`).
  - State changes occur exclusively via domain methods carrying business intent (`addRound(...)`, `recordEvent(...)`).
  - Entity `equals` and `hashCode` are implemented using immutable business keys (UUID / natural code), never database auto-increment keys.

### DEC-004: Column Name `match_minute` vs. Reserved Keyword `minute`
- **Context**: `minute` is a reserved SQL keyword in H2 database engines used in test profiles.
- **Decision**: The database column is explicitly named `match_minute` in migration DDL and mapped to `private int matchMinute;` with getter `getMatchMinute()` in the Java domain model.

### DEC-005: Shared Agent Memory Framework
- **Context**: Seamless interoperability between OMP (active) and Claude Code (future) without duplicate instructions or divergent operational rules.
- **Decision**: `AGENTS.md` acts as the single master source of truth. `CLAUDE.md` links directly to `AGENTS.md` and provides platform-specific Windows terminal command wrappers.
