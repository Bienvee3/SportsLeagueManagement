# Current Project Status — Sports League Management

**Last Updated**: 2026-11-07
**Active Branch**: `main`
**Latest Local Commit**: `b018bdc feat(web): add REST endpoints to list, create and delete rounds and matches`
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
- **Phase 7**: Spring Security 7 + V3 migration (users/authorities) + JDBC user store + InitialUserSeeder + login page ✅
- **Phase 8**: REST API controllers (Leagues, Teams, Seasons, Fixtures) + springdoc OpenAPI + ProblemDetail RFC 7807 ✅
- **Phase 9**: Standings engine (5-level tie-break) + `StandingsCalculator` + `SeasonService.getStandings` + REST endpoint ✅
- **Phase 10**: Gemini AI chronicle (Spring AI 2.0.2, hexagonal port + adapter) + `ChronicleService` + `POST /chronicle` ✅
- **Phase 11**: JaCoCo ≥80% gate + Mockito Java 25 `-javaagent` fix + Testcontainers integration profile ✅
- **Phase 12 (frontend)**: Thymeleaf CRUD views — Layout Dialect, Dashboard, Leagues, Seasons, Teams, Fixtures, error pages, 15 WebMvcTest tests ✅
---

## 2. Milestone Progress Overview

| Milestone | Scope | Status | Progress |
| :---: | :--- | :---: | :---: |
| **M1** | JPA Domain Model, Flyway V1+V2, Docker Compose, ADR 001, Tests | **COMPLETED** | 100% |
| **M2** | Restructuring, Repos, Services, DTOs, Round-Robin, Thymeleaf | **COMPLETED** | 100% |
| **M4** | JaCoCo ≥80%, Mockito fix, Testcontainers integration profile | **COMPLETED** | 100% |

---

## 3. Completed Restructuring Phases

### Phase 8 — REST API + OpenAPI + ProblemDetail (`review/phase-8`)
**Commit**: `6f116c3 feat(web): add REST API controllers, OpenAPI/Swagger UI, and ProblemDetail error handler`

| File | Description |
|---|---|
| `pom.xml` | Added `springdoc-openapi-starter-webmvc-ui:3.1.1` |
| `OpenApiConfig` | `OpenAPI` bean with project title/version |
| `MethodSecurityConfig` | `@EnableMethodSecurity` (separated from SecurityConfig to avoid bean init cycle) |
| `SecurityConfig` | Extended with Swagger UI public paths |
| `LeagueRestController` | GET list/by-id/by-slug, POST, PUT, DELETE |
| `TeamRestController` | GET list/by-id, POST, PUT |
| `SeasonRestController` | GET list/by-id, POST create/activate/complete/register-team/withdraw-team/schedule |
| `FixtureRestController` | GET by-id/by-round/by-season, POST result/correct-result/postpone/cancel/reschedule/events |
| `RestExceptionHandler` | `@RestControllerAdvice` with ProblemDetail for 400/404/409/500; re-throws `AccessDeniedException` |
| `*RestControllerTest` ×4 | `@WebMvcTest` slices — 34 tests covering 200/201/204/400/401/403/404/409 |

**Swagger UI**: `/swagger-ui` (public), OpenAPI spec: `/api-docs` (public).  
**Role enforcement**: `@PreAuthorize` on all mutation endpoints (ADMIN for leagues/teams/seasons, ADMIN+OFFICIAL for fixtures).

**Deliverables completed**:

| File | Description |
|---|---|
| `V3__create_security_tables.sql` | `users` + `authorities` tables (Spring Security standard schema) |
| `SecurityConfig` | `SecurityFilterChain`: form login `/login`, BCrypt, 3 role rules (ADMIN/OFFICIAL/VIEWER) |
| `InitialUserSeeder` | `ApplicationRunner`: reads `ADMIN_USERNAME`/`ADMIN_PASSWORD`, fail-fast if blank, idempotent |
| `.env.example` | Env var template at repo root |
| `LoginController` + `login.html` | Custom login page |
| `InitialUserSeederTest` (4) | Mockito unit — fail-fast + idempotency |
| `SecurityAccessTest` (7) | `@SpringBootTest` + MockMvc — redirect, public login, role rules |

**V3 migration**: Flyway validated 3 migrations successfully on H2 test profile.


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

**Tests**: 300/300 ✅ (Phase 9 adds StandingsCalculatorTest 18 + 2 SeasonService getStandings tests)

---

## 4. Test Suite — 355/355

```
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd test"
Tests run: 355, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (2026-11-07)
```

| Test Class | Count | Type |
|---|---|---|
| `FixtureServiceTest` | 12 | Mockito unit |
| `SeasonServiceTest` | 17 | Mockito unit |
| `LeagueServiceTest` | 11 | Mockito unit |
| `TeamServiceTest` | 6 | Mockito unit |
| `InitialUserSeederTest` | 5 | Mockito unit |
| `SlugTest` | 23 | Pure unit |
| `StateTransitionGuardTest` | 37 | Pure unit |
| `TeamCodeTest` | 19 | Pure unit |
| `DomainMethodsTest` | 17 | Pure unit |
| `MatchMinuteTest` | 13 | Pure unit |
| `DomainModelTest` | 3 | Pure unit |
| `DomainExceptionsTest` | 4 | Pure unit |
| `ScoreTest` | 9 | Pure unit |
| `RepositorySliceTest` | 21 | `@DataJpaTest` |
| `SecurityAccessTest` | 7 | `@SpringBootTest` + MockMvc |
| `SportsLeagueManagementApplicationTests` | 1 | `@SpringBootTest` |
| `RoundRobinSchedulerTest` | 44 | Pure unit |
| `LeagueRestControllerTest` | 10 | `@WebMvcTest` |
| `TeamRestControllerTest` | 8 | `@WebMvcTest` |
| `SeasonRestControllerTest` | 8 | `@WebMvcTest` |
| `FixtureRestControllerTest` | 8 | `@WebMvcTest` |
| `StandingsCalculatorTest` | 18 | Pure unit |
---

## 5. Current Package Structure

```
com.league.sportsleague/
├── application/
│   ├── dto/        ← 13 Java Records (request + response + StandingsResponse)  ✅
│   └── service/    ← LeagueService, TeamService, SeasonService, FixtureService  ✅
├── domain/
│   ├── model/      ← 8 entities + enums
│   ├── valueobject/← Slug, TeamCode, MatchMinute, Score
│   ├── exception/  ← InvalidStateTransition, DuplicateTeam, EntityNotFound
│   ├── repository/ ← 5 domain interfaces
│   ├── StandingsEntry.java  ← immutable standings row record  ✅
│   └── StandingsCalculator.java  ← pure domain service, 5-level tie-break  ✅
│   ├── model/      ← 8 entities + enums
│   ├── valueobject/← Slug, TeamCode, MatchMinute, Score
│   ├── exception/  ← InvalidStateTransition, DuplicateTeam, EntityNotFound
│   └── repository/ ← 5 domain interfaces
├── infrastructure/
│   ├── config/     ← InitialUserSeeder, OpenApiConfig  ✅
│   ├── security/   ← SecurityConfig, MethodSecurityConfig  ✅
│   └── persistence/
│       ├── converter/     ← 3 AttributeConverters
│       └── Jpa*Repository ← 5 Spring Data implementations
└── web/
    ├── api/        ← LeagueRestController, TeamRestController, SeasonRestController, FixtureRestController, ChronicleRestController, RestExceptionHandler  ✅
    └── view/       ← LoginController, DashboardController, LeagueViewController, SeasonViewController, TeamViewController, FixtureViewController, GlobalViewExceptionHandler  ✅
```

---

## 6. Project Status — All Phases Complete + UX Improvements

**M1, M2, M3, M4 ALL COMPLETED. Branch `main` is current.**

All features implemented and verified with 326/326 tests:
- Domain model, value objects, exceptions, guards ✅
- Application services + DTOs ✅
- Round-Robin scheduler + `Season.clearRounds()` for regeneration ✅
- Spring Security 7 (3 roles, JDBC user store) ✅
- `SwitchUserFilter` dev role preview (switch to test-official / test-viewer from topbar) ✅
- REST API (Leagues, Teams, Seasons, Fixtures) + OpenAPI/Swagger UI ✅
- Standings engine (5-level tie-break) ✅
- AI Chronicle via Gemini API + stub fallback on API failure (no more 500) ✅
- JaCoCo ≥80%, Mockito Java 25 fix, Testcontainers integration profile ✅
- Thymeleaf CRUD frontend: Layout Dialect, Dashboard, Leagues, Seasons, Teams, Fixtures, error pages ✅
- Richer UI: sidebar gradient, topbar blur/glassmorphism, card shadows, event timeline classes ✅

**Testcontainers**: `mvnw verify -Pintegration` — requires Docker Desktop running.
**AI Chronicle**: set `GEMINI_API_KEY` env var; falls back to stub if key invalid/missing.
**Dev role switcher**: `test-official` / `test-viewer` seeded at startup; switch via topbar user menu.
**Next**: Push `main` to origin / open PR (awaiting explicit user authorization per AGENTS.md §6.4).

## 7. Session 2026-11-07
- Audit fixes `4ad9503`, `fde7829`: V4 migration (`ROLE_SWITCH_USER`), schedule `flush()`, button guard, error handlers, Calendar link for all roles.
- `af43a6c`: `SwitchUserFilter` success/failure handlers (Security 7 NPE → 500 on role switch); +2 tests in `SecurityAccessTest`.
- `1d0f69b`: full UI redesign (dark sports theme, fixed topbar, no sidebar, per-role views); `DashboardController` real KPIs; fixed `fixtures/detail` minute expression (500 on any fixture with events).
- Smoke-verified at runtime: all pages 200 as ADMIN/OFFICIAL/VIEWER; OFFICIAL/VIEWER get 403 on create routes; switch/exit round-trip OK.
- Dev DB: `league_admin` password aligned with `.env` (`leagueadmin`); `application.yml` dev default (`league_dev_secret`) still differs from `.env`.
- `b730d41`: root cause of dead dropdown/tabs/role switch = invalid SRI hash on `bootstrap.bundle.js` (script blocked). Removed; topbar now has visible segmented "View as Admin/Official/Viewer" switcher (JS: exit then switch via fetch) and a Sign out button.
- `c08fab5`: `Team.sportType` (immutable, default FOOTBALL, V5 migration), sport dropdown on create, sport column + filter pills in `/teams`, season team dropdown limited to league sport, `SeasonService.registerTeam` rejects sport mismatch. +6 tests.
- Browser-verified with real clicks: role switch, Actions tab, season Teams tab, team creation, Sign out.
- `210d94c`: stadium/city marked required in team form.
- `9f7ddea`: AI chronicle now available to VIEWER (REST `@PreAuthorize` widened) and rendered inline on the fixture page (no raw JSON tab).
- `18e94be`: `SeasonService.createRound/addFixture/findRounds`; calendar page lets ADMIN create the next round and add matches one by one (teams must be CONFIRMED and free in that round; blocked on COMPLETED seasons). Busy-team check compares ids (Hibernate proxies broke `equals`). +5 tests.
- Not yet in UI: delete/edit a round or a match; REST endpoints for round/match creation.
- `8c5375a`: season Teams tab explains an empty "Add team" list (only unregistered teams of the league's sport are offered).
- `86d900a`: `SeasonService.deleteFixture/deleteRound` + calendar trash buttons (ADMIN). FINISHED matches (and rounds containing them) cannot be deleted; round numbers are not renumbered after a delete (gaps allowed). +4 tests.
- `68f7437`: rounds are auto-numbered (`createRound(seasonId, startDate)`, no name field); `deleteRound` flushes the delete then renumbers later rounds one by one (`Round.renumber`, default "Round N" names follow). Verified on PostgreSQL.
- `b018bdc`: REST `GET/POST /api/leagues/{l}/seasons/{s}/rounds`, `DELETE .../rounds/{r}`, `POST .../rounds/{r}/fixtures`, `DELETE .../fixtures/{f}`; `RestExceptionHandler` maps `IllegalStateException`→409 and `IllegalArgumentException`→400.
- AI chronicle: `gemini-2.5-flash` was retired by Google (404). Now `gemini-3.5-flash`, `max-output-tokens: 2048` (thinking model), adapter fallback text no longer claims "not configured" and logs the root cause. Verified live in EN and HR. Note: Google returns transient 503 "high demand" on some models; retry works.
- App language switch: EN/HR slider in the top bar (`?appLang=en|hr`, cookie `app-lang`, `LanguageConfig`). The fixture page has a single "Generate" button that produces the chronicle in the active language. UI labels are NOT translated (only the AI chronicle follows the language). Param is `appLang` because the chronicle REST endpoint already uses `?lang=`. Tests: 358/358.
- Full UI i18n (EN default / HR): `LanguageConfig` `MessageSource` (bundles `i18n/{common,pages,leagues,teams,seasons,fixtures}[_hr].properties`), EN/HR button pair in the top bar (`?appLang=`, cookie `app-lang`), all templates use `#{key}`; static controller flash messages use `flashKey`/`errorKey`. Chronicle Generate button is just "Generate/Generiraj" and generates in the active language. `I18nBundlesTest` guards EN/HR key parity and that every `#{key}` used in a template exists. Not translated: data (names), dynamic exception messages (`e.getMessage()`), duplicate-season-name error. Tests: 360/360.
