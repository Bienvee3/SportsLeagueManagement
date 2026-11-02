# Current Project Status — Sports League Management

**Last Updated**: 2026-11-02  
**Active Branch**: `review/phase-6` (local, unpushed)  
**Latest Local Commit**: `13dd98a feat(service): add generateSchedule to SeasonService`  
**Git Working Tree**: Clean.  
**Push to origin**: Not yet — awaiting explicit user authorization per AGENTS.md §6.4.

---

## 1. Executive Summary

Milestone 1 is 100% complete. Architecture restructuring Phases 1–5 are complete on `review/phase-5`:

- **Phase 1**: Package reorganization ✅
- **Phase 2**: Value Objects ✅
- **Phase 3**: Domain exceptions + guards + CQS ✅
- **Phase 4**: JPA Repository contracts + Spring Data implementations ✅
- **Phase 5**: Application Services + DTOs ✅
- **Phase 6**: Round-Robin Scheduler (domain + SeasonService) ✅
- **Phase 7**: Spring Security 7 + V3 migration — **NEXT (M3)**

---

## 2. Milestone Progress Overview

| Milestone | Scope | Status | Progress |
| :---: | :--- | :---: | :---: |
| **M1** | JPA Domain Model, Flyway V1+V2, Docker Compose, ADR 001, Tests | **COMPLETED** | 100% |
| **M2** | Restructuring, Repos, Services, DTOs, Round-Robin, Thymeleaf | **IN PROGRESS** | ~65% |
| **M3** | Spring Security 7, REST API + OpenAPI, AI Chronicle | ⏳ Pending | 0% |
| **M4** | JaCoCo ≥80%, i18n, Quartz, Deployment | ⏳ Pending | 0% |

---

## 3. Completed Restructuring Phases

### Phase 6 — Round-Robin Scheduler (`review/phase-6`)
**2 atomic commits**:

| Commit | Hash | Content |
|---|---|---|
| `feat(domain): implement round-robin schedule generation with greedy home-away balance` | `85fdeab` | `RoundRobinScheduler` (circle method, single round-robin), 44 tests |
| `feat(service): add generateSchedule to SeasonService` | `13dd98a` | `generateSchedule(seasonId, firstMatchTime)`: CONFIRMED teams only, `season.addRound`, cascade save; 3 tests |

**Scheduler rules**: N-1 circle rounds (odd N adds BYE); each pair plays once; no team plays twice per round; home/away diff ≤ 1 via greedy tracker; round dates weekly (+7 days); all fixtures in a round share the round time.

**Known gap**: `generateSchedule` does not reject a season that already has rounds; calling it twice duplicates fixtures. Guard not yet decided.

### Phase 5 — Application Services + DTOs (`review/phase-5`)
**5 atomic commits**:

| Commit | Hash | Content |
|---|---|---|
| `feat(service): add DTOs` | `ac67eb2` | 12 Java Records with Bean Validation |
| `feat(service): add LeagueService` | `7a81df8` | createLeague, rename, delete, find* |
| `feat(service): add TeamService` | `698b8eb` | createTeam, updateProfile, find* |
| `feat(service): add SeasonService` | `202ccef` | createSeason, activate, complete, register/withdrawTeam, completeRound, find* |
| `feat(service): add FixtureService` | `f3c8c1f` | recordResult, correctResult, postpone, cancel, reschedule, recordMatchEvent, find* |

**Service pattern applied in all 4**:
- `@Service`, `@Transactional` class-level (commands), `@Transactional(readOnly=true)` per query method
- Constructor injection of domain repository interfaces only — no Spring Data imports in services
- `EntityNotFoundException` when `findById` returns `Optional.empty()`
- Static `from(Entity)` mapper methods on response DTOs — no separate mapper class

**Tests**: 237/237 ✅ (Phase 6 adds RoundRobinSchedulerTest 44 + 3 SeasonService schedule tests)

---

## 4. Test Suite — 237/237

```
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd test"
Tests run: 237, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

| Test Class | Count | Type |
|---|---|---|
| `FixtureServiceTest` | 12 | Mockito unit |
| `SeasonServiceTest` | 17 | Mockito unit |
| `LeagueServiceTest` | 11 | Mockito unit |
| `TeamServiceTest` | 6 | Mockito unit |
| `SlugTest` | 23 | Pure unit |
| `StateTransitionGuardTest` | 37 | Pure unit |
| `TeamCodeTest` | 19 | Pure unit |
| `DomainMethodsTest` | 17 | Pure unit |
| `RepositorySliceTest` | 21 | @DataJpaTest |
| `MatchMinuteTest` | 13 | Pure unit |
| `DomainModelTest` | 3 | Pure unit |
| `DomainExceptionsTest` | 4 | Pure unit |
| `ScoreTest` | 9 | Pure unit |
| `SportsLeagueManagementApplicationTests` | 1 | @SpringBootTest |

---

## 5. Current Package Structure

```
com.league.sportsleague/
├── application/
│   ├── dto/        ← 12 Java Records (request + response)  ✅
│   └── service/    ← LeagueService, TeamService, SeasonService, FixtureService  ✅
├── domain/
│   ├── model/      ← 8 entities + enums
│   ├── valueobject/← Slug, TeamCode, MatchMinute, Score
│   ├── exception/  ← InvalidStateTransition, DuplicateTeam, EntityNotFound
│   └── repository/ ← 5 domain interfaces
└── infrastructure/
    └── persistence/
        ├── converter/     ← 3 AttributeConverters
        └── Jpa*Repository ← 5 Spring Data implementations
```

---

## 6. Immediate Next Steps — Phase 7 (M3 starts)

**Open Phase 6 items**: decide the regeneration guard for `generateSchedule`; decide whether round dates should vary kickoff times per fixture.

**Target**: Spring Security 7 configuration + user table migration

**Deliverables:**
1. Flyway `V3__add_users_table.sql` — `users` + `authorities` tables (Spring Security standard schema)
2. `infrastructure.security.SecurityConfig` — `SecurityFilterChain` with form login, BCrypt, 3 role rules
3. `infrastructure.config.InitialUserSeeder` — `ApplicationRunner` reading `ADMIN_USERNAME`/`ADMIN_PASSWORD` from env, fail-fast if missing, idempotent
4. `.env.example` at repo root documenting required env vars
5. `web.view.LoginController` + `login.html` template
6. Security slice tests (`@WebMvcTest`)

**Pending decisions before Phase 6:**
- Confirm `thymeleaf-extras-springsecurity6` compatibility with Spring Security 7 (may need `-springsecurity7` artifact)
