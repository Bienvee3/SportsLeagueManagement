# System Architecture — Sports League Management

Layered architecture with Ports and Adapters for the one external integration (AI). Everything below is
implemented and verified by `mvnw verify` (633 tests) unless marked otherwise. Diagrams: `docs/uml/` (domain,
aggregate chain, states, runtime environments). Decisions: `docs/adr/`.

## 1. Layers

```
   Browser ── HTTPS ──►  Web layer      Thymeleaf views + REST controllers (/api/**, OpenAPI at /api-docs)
                                        controllers: HTTP binding, Bean Validation, POST-Redirect-GET, ProblemDetail
                                │
                                ▼
                         Application    @Service classes (@Transactional), DTO records, PageQuery/PageResponse
                                        @PreAuthorize on every write method (ADMIN, or ADMIN+OFFICIAL)
                                │
                                ▼
                         Domain         rich entities and invariants, value objects (Slug, TeamCode, MatchMinute, Score),
                                        StandingsCalculator, round-robin generator, repository + AI port interfaces
                                │
                 ┌──────────────┴───────────────┐
                 ▼                              ▼
          Infrastructure                 Infrastructure
          Spring Data JPA / Hibernate 7  GeminiChronicleAdapter (Spring AI) | StubChronicleAdapter
          PostgreSQL 17 + Flyway         Quartz jobs, security config, rate limiter
```

Rules enforced in code and tests: constructor injection only; entities never leave the application layer (DTOs
and `ProblemDetail` only); equality by business key; no setters, state changes through domain methods;
`ddl-auto=validate`, schema only through Flyway (V1 to V11).

## 2. Domain (`com.league.sportsleague.domain`)

Aggregate chain `League → Season → Round → Fixture → Team`, plus `Fixture → MatchEvent`, `Fixture → ResultAudit`,
`Team → Player`, `Fixture → LineupEntry`, `Fixture → MatchChronicle` (one per fixture and language).

* `Fixture` owns the match rules: lifecycle `SCHEDULED → IN_PROGRESS → FINISHED` (also `POSTPONED`, `CANCELLED`);
  the score of a live match is the sum of its goal events; a finished score changes only through
  `correctResult`, which writes a `ResultAudit`.
* `Round.advanceFromFixtures` derives the round status from its matches (used by the scheduler).
* `StandingsCalculator` derives the table from finished fixtures every time: points, goal difference, goals for,
  then head-to-head points and goal difference. A late correction therefore reorders the table with no extra code.
* Round-robin scheduling (circle method) gives every team one match per round with balanced home and away.

## 3. Application layer

`LeagueService`, `TeamService`, `SeasonService`, `FixtureService` hold the transactions. Reads are
`@Transactional(readOnly = true)` and use `JOIN FETCH` or `@BatchSize` (`docs/performance.md`: 19 statements → 1).
`ChronicleService` is deliberately not transactional: the AI call runs between two short transactions owned by
`ChronicleStore`, so no pooled connection waits on the provider. `RoundLifecycleService` and
`OverdueFixtureService` are called by the scheduler and carry no role check (there is no user).

## 4. Security

Form login, CSRF, BCrypt, three roles: `ADMIN` (administration), `OFFICIAL` (match events, results, AI), `VIEWER`
(read-only). Rules exist twice on purpose: on controllers (HTTP answer 403) and on service methods
(`ServiceAuthorizationTest`), so a new caller of a service cannot skip them. Users come from `.env`/environment
at first start. Production: `Secure`, `HttpOnly`, `SameSite=Lax` session cookie; HSTS and `X-Frame-Options` from
the platform and Spring Security. Public: login, `/actuator/health`, `/api-docs`, Swagger UI.

## 5. AI chronicles (details in ADR 003)

`ChroniclePort` is the only thing the domain knows. The request carries teams, score, ordered events and the
effect on the table. Every answer is verified by `ChronicleValidator` (final score, only scorelines derivable from
the events, no leaked drafting notes, length); one corrected retry, then 503 and nothing stored. `AiCallLimiter`
caps provider calls per hour. The test and `none` configurations use a stub that throws instead of inventing text.

## 6. Scheduling

Quartz with a clustered JDBC job store (tables in V11): `RoundLifecycleJob` every 5 minutes advances round
statuses; `OverdueFixtureJob` hourly logs matches that should have started.

## 7. Deployment

`Dockerfile` (multi-stage, non-root) → Render (Docker, free plan) with Neon PostgreSQL. `render.yaml` is the
Blueprint; secrets live only in the Render dashboard and **`SPRING_PROFILES_ACTIVE=prod` is mandatory** (without it the
dev profile runs, without `Secure` cookies). GitHub Actions runs `mvnw verify` and the Testcontainers profile.

## 8. Not implemented

* Configurable tie-break order (it is fixed in `StandingsCalculator`).
* Edit forms for Season, Round and Fixture (they can be created and deleted).
* Distributed rate limiting (the AI limiter is in memory, per instance).
* The Figma prototype (student deliverable).
