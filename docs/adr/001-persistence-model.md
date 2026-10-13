# ADR 001: Relational Persistence Model with PostgreSQL, Spring Data JPA, and Flyway

## Status
Accepted

## Date
2026-10-13

## Authors
- Doalto Cavada Bienvenido (Student)
- System Architecture Team

---

## Context and Problem Statement
The **P39 Sports League Management** system models a multi-tier sports domain structured as a strict aggregate chain:
$$\text{League} \longrightarrow \text{Season} \longrightarrow \text{Round} \longrightarrow \text{Fixture} \longrightarrow \text{Team} \quad \text{and} \quad \text{Fixture} \longrightarrow \text{MatchEvent}$$

Key functional and architectural requirements include:
1. **Relational Integrity**: Enforcing strict uniqueness, foreign keys, and constraints (e.g., home and away teams cannot be identical; a team cannot play twice in the same round).
2. **Advanced Tie-Break Recalculations**: Head-to-head point calculations and goal differentials dynamically evaluated across rounds.
3. **Audited Result Corrections**: Any historical score update cascades into retroactive standings recalculations while retaining an immutable audit trail (`ResultAudit`).
4. **Academic & Production Constraints**: Mandatory compliance with LO3 and LO4 (Hibernate 7, zero auto-generated schemas in final submission, strict versioned migrations).

We needed to decide on the database engine, the ORM mapping strategy, and the schema evolution mechanism.

---

## Decision Drivers
* **ACID Transactions**: Standings recomputation and audit logging must be atomic.
* **Complex Multi-Table Querying**: Standings and tie-break rules require aggregations, joins, and relational algebra.
* **Deterministic Schema Evolution**: Zero risk of unintentional DDL changes in staging or production.
* **Explicit Connection Management**: Predictable HikariCP connection pooling suitable for cloud deployment (Render, Neon).

---

## Considered Options
1. **Option 1 (Chosen)**: PostgreSQL 17 + Spring Data JPA (Hibernate 7) + Flyway database migrations (`spring.jpa.hibernate.ddl-auto=validate`).
2. **Option 2**: PostgreSQL + Spring Data JPA with Hibernate DDL auto-generation (`ddl-auto=update` or `create-drop`).
3. **Option 3**: Document Database (MongoDB) with Spring Data MongoDB.

---

## Decision Outcome
We chose **Option 1: PostgreSQL 17 + Spring Data JPA over Hibernate 7 with Flyway-managed schema migrations**.

### Justification:
* **Relational Rigor & Constraints**: PostgreSQL natively provides foreign keys, unique compound constraints, and check constraints (`chk_fixtures_different_teams`), preventing invalid state from ever entering the persistence store.
* **Controlled Schema Evolution**: Flyway executes reproducible, versioned SQL scripts (`V1__init_schema.sql`, `V2__seed_initial_data.sql`). Hibernate is restricted to `validate` mode, preventing accidental table alterations in production and satisfying academic LO3 requirements.
* **Rich Domain Modeling**: Spring Data JPA enables expressive entity modeling with lazy loading (`FetchType.LAZY`), bidirectional convenience methods, and association entities (`SeasonTeam`) with custom attributes.
* **Audit Trail Reliability**: PostgreSQL relational transactions ensure that updating a fixture score and recording a `ResultAudit` entry execute atomically within a single database transaction.

---

## Pros and Cons of the Options

### Option 1: PostgreSQL + Spring Data JPA + Flyway (Chosen)
* **Good**: Guaranteed relational integrity, ACID compliance, and full traceability of schema changes.
* **Good**: Explicit connection pool configuration via HikariCP, tuned for containerized cloud deployment.
* **Good**: Prevents schema drift across development, CI/CD, and production environments.
* **Bad / Trade-off**: Requires writing and maintaining SQL migration scripts by hand for each domain modification.
* **Bad / Trade-off**: Requires conscious query design (fetch joins / entity graphs) to avoid Hibernate N+1 query overhead.

### Option 2: Hibernate DDL Auto-Generation (`ddl-auto=update`)
* **Good**: Fast rapid prototyping without writing migration scripts.
* **Bad**: High risk of data loss or silent column corruption in production.
* **Bad**: Does not manage indexes or custom database constraints cleanly.
* **Bad**: Explicitly prohibited by the course grading criteria for final submission.

### Option 3: Document Database (MongoDB)
* **Good**: Schema flexibility and fast single-document reads.
* **Bad**: Denormalization creates severe data anomalies when historical fixture scores are corrected weeks later.
* **Bad**: Lack of native join operations makes head-to-head tie-break queries computationally expensive and error-prone.
* **Bad**: Violates the course specification for LO3 and LO4.

---

## Accepted Consequences
1. **Mandatory Flyway Script Authoring**: Any future schema change must be introduced via incremental Flyway versioned scripts (e.g., `V3__...sql`), never through JPA annotations alone.
2. **Performance Monitoring**: Because entities use `FetchType.LAZY` by default to avoid cartesian explosions, service methods that load parent-child aggregates must explicitly define `@Query` with fetch joins or `@EntityGraph`, which will be demonstrated and documented in Milestone 4.

