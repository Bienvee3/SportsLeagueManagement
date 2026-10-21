# Development & Clean Code Guidelines

This document outlines the software craft standards, Git commit policies, and clean code practices enforced for the **P39 Sports League Management** project.

---

## 1. Atomic Commits & Git Workflow

### 1.1 Atomic Commits
* **Single Logical Change**: Every commit must represent exactly one logical unit of work (one feature slice, one bug fix, one refactor, or one test addition).
* **Always Green**: The codebase must compile and pass all tests at every commit. Never commit broken intermediate states.
* **Separation of Concerns**: Never mix refactoring with new functional behavior or formatting changes with bug fixes.

### 1.2 Commit Message Convention (Conventional Commits)
All commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short summary in imperative present tense>
```

* **Types**:
  * `feat`: New functional behavior or domain capability.
  * `fix`: Bug fix or defect correction.
  * `refactor`: Code reorganization that neither fixes a bug nor adds a feature.
  * `test`: Adding missing tests or correcting existing tests without modifying production code.
  * `docs`: Documentation updates (ADRs, README, guidelines).
  * `perf`: Performance optimizations (e.g., query tuning, cache integration).
  * `chore`: Build tooling, dependency bumps, or repository configuration.
* **Scopes**: `domain`, `db`, `repo`, `service`, `security`, `web`, `ai`, `infra`, `test`.
* **Example**:
  * `feat(standings): implement dynamic head-to-head tie-break comparator`
  * `test(round-robin): add unit tests for home-away fixture scheduling`
  * `refactor(domain): encapsulate season team registration within Season aggregate`

---

## 2. Clean Code & Architectural Principles

### 2.1 Object-Oriented & Domain Design
* **Rich Domain Model**: Keep business logic within aggregates and domain entities rather than anemic models with public setters.
* **Encapsulation & Defensive Copies**:
  * Expose immutable view collections via `Collections.unmodifiableList(...)`.
  * Protect internal collections through dedicated domain methods (`addRound(...)`, `recordEvent(...)`).
* **Invariants Enforcement**: Validate state transitions and mandatory parameters at entity boundary methods (e.g., reject nulls, forbid duplicate teams, ensure home/away distinction).
* **Business Key Equality**: Entity `equals` and `hashCode` must be based on immutable business keys (e.g., UUID or unique business identity), never database-generated synthetic IDs.

### 2.2 Clean Code Standards
* **Meaningful Names**: Intent-revealing names for classes, variables, and methods. Avoid cryptograms, meaningless abbreviations, or hungarian notation.
* **Single Responsibility Principle (SRP)**:
  * Classes and methods must do one thing well.
  * Keep methods short (< 25 lines ideally) and focused.
* **Fail Fast & Explicit Exceptions**:
  * Throw domain-specific exceptions (e.g., `InvalidFixtureException`, `SeasonClosedException`) instead of generic `RuntimeException`.
  * Validate preconditions immediately.
* **No Magic Values**: Replace literal numbers and strings with well-named constants, enums, or value objects.
* **Zero Dead Weight**:
  * No commented-out code.
  * Remove unused imports, variables, and obsolete methods immediately.
  * No `TODO` placeholders in submitted work.

### 2.3 Layered & Hexagonal Architecture
* **Clear Boundary Separation**:
  * Domain layer must not depend on web or database frameworks.
  * Application/Service layer coordinates transactions and business use cases.
  * AI module must adhere strictly to **Ports & Adapters (Hexagonal)**: define domain ports (interfaces) and isolate external LLM integrations in adapter packages.
* **Query Performance**:
  * Maintain `FetchType.LAZY` default.
  * Avoid N+1 query problems via explicit `@Query` fetch joins or `@EntityGraph`.

### 2.4 Testing Discipline
* **AAA Pattern**: Every test structured cleanly into **Arrange**, **Act**, and **Assert**.
* **Isolated & Deterministic**: Tests must be independent of execution order and external environment state.
* **Threshold**: Maintain $\ge 80\%$ line coverage on the service layer via JaCoCo.
