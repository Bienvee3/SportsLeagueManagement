# Current Project Status — Sports League Management

**Last Updated**: 2026-11-07
**Active Branch**: `main`
**Latest Local Commit**: `b880110 feat(domain): synchronize fixture score from goal events`
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

## 4. Test Suite — 376/376

```
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd test"
Tests run: 376, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS (2026-11-07)
```

| Test Class | Count | Type |
|---|---|---|
| `FixtureServiceTest` | 18 | Mockito unit |
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
- REST API (Leagues, Teams, Seasons, Fixtures) + OpenAPI/Swagger UI ✅
- Standings engine (5-level tie-break) ✅
- AI Chronicle via Gemini API + stub fallback on API failure (no more 500) ✅
- JaCoCo ≥80%, Mockito Java 25 fix, Testcontainers integration profile ✅
- Thymeleaf CRUD frontend: Layout Dialect, Dashboard, Leagues, Seasons, Teams, Fixtures, error pages ✅
- Richer UI: sidebar gradient, topbar blur/glassmorphism, card shadows, event timeline classes ✅

**Testcontainers**: `mvnw verify -Pintegration` — requires Docker Desktop running.
**AI Chronicle**: set `GEMINI_API_KEY` env var; falls back to stub if key invalid/missing.
**Accounts**: `ADMIN_*`, `OFFICIAL_*`, `VIEWER_*` variables in `.env` (see `.env.example`); created at startup when absent, never overwritten.
**Next**: Push `main` to origin / open PR (awaiting explicit user authorization per AGENTS.md §6.4).

## 7. Session 2026-11-07
- Audit fixes `4ad9503`, `fde7829`: V4 migration (`ROLE_SWITCH_USER`), schedule `flush()`, button guard, error handlers, Calendar link for all roles.
- `af43a6c`: `SwitchUserFilter` success/failure handlers (Security 7 NPE → 500 on role switch); +2 tests in `SecurityAccessTest`.
- `1d0f69b`: full UI redesign (dark sports theme, fixed topbar, no sidebar, per-role views); `DashboardController` real KPIs; fixed `fixtures/detail` minute expression (500 on any fixture with events).
- Smoke-verified at runtime: all pages 200 as ADMIN/OFFICIAL/VIEWER; OFFICIAL/VIEWER get 403 on create routes; switch/exit round-trip OK.
- Dev DB: `league_admin` password aligned with `.env` (`leagueadmin`). (Superseded 2026-11-08: the dev profile no longer carries a password literal, section 8.)
- `b730d41`: root cause of dead dropdown/tabs/role switch = invalid SRI hash on `bootstrap.bundle.js` (script blocked). Removed; topbar now has visible segmented "View as Admin/Official/Viewer" switcher (JS: exit then switch via fetch) and a Sign out button.
- `c08fab5`: `Team.sportType` (immutable, default FOOTBALL, V5 migration), sport dropdown on create, sport column + filter pills in `/teams`, season team dropdown limited to league sport, `SeasonService.registerTeam` rejects sport mismatch. +6 tests.
- Browser-verified with real clicks: role switch, Actions tab, season Teams tab, team creation, Sign out.
- `210d94c`: stadium/city marked required in team form.
- `9f7ddea`: AI chronicle rendered inline on the fixture page (no raw JSON tab). Its original "available to VIEWER" generation was reverted on 2026-11-08: VIEWER can only read stored chronicles (section 8).
- `18e94be`: `SeasonService.createRound/addFixture/findRounds`; calendar page lets ADMIN create the next round and add matches one by one (teams must be CONFIRMED and free in that round; blocked on COMPLETED seasons). Busy-team check compares ids (Hibernate proxies broke `equals`). +5 tests.
- Not yet in UI: delete/edit a round or a match; REST endpoints for round/match creation.
- `8c5375a`: season Teams tab explains an empty "Add team" list (only unregistered teams of the league's sport are offered).
- `86d900a`: `SeasonService.deleteFixture/deleteRound` + calendar trash buttons (ADMIN). FINISHED matches (and rounds containing them) cannot be deleted; round numbers are not renumbered after a delete (gaps allowed). +4 tests.
- `68f7437`: rounds are auto-numbered (`createRound(seasonId, startDate)`, no name field); `deleteRound` flushes the delete then renumbers later rounds one by one (`Round.renumber`, default "Round N" names follow). Verified on PostgreSQL.
- `b018bdc`: REST `GET/POST /api/leagues/{l}/seasons/{s}/rounds`, `DELETE .../rounds/{r}`, `POST .../rounds/{r}/fixtures`, `DELETE .../fixtures/{f}`; `RestExceptionHandler` maps `IllegalStateException`→409 and `IllegalArgumentException`→400.
- AI chronicle: `gemini-2.5-flash` was retired by Google (404). Now `gemini-3.5-flash`, `max-output-tokens: 2048` (thinking model), adapter fallback text no longer claims "not configured" and logs the root cause. Verified live in EN and HR. Note: Google returns transient 503 "high demand" on some models; retry works.
- App language switch: EN/HR slider in the top bar (`?appLang=en|hr`, cookie `app-lang`, `LanguageConfig`). The fixture page has a single "Generate" button that produces the chronicle in the active language. UI labels are NOT translated (only the AI chronicle follows the language). Param is `appLang` because the chronicle REST endpoint already uses `?lang=`. Tests: 358/358.
- Full UI i18n (EN default / HR): `LanguageConfig` `MessageSource` (bundles `i18n/{common,pages,leagues,teams,seasons,fixtures}[_hr].properties`), EN/HR button pair in the top bar (`?appLang=`, cookie `app-lang`), all templates use `#{key}`; static controller flash messages use `flashKey`/`errorKey`. Chronicle Generate button is just "Generate/Generiraj" and generates in the active language. `I18nBundlesTest` guards EN/HR key parity and that every `#{key}` used in a template exists. Not translated: data (names), dynamic exception messages (`e.getMessage()`), duplicate-season-name error. Tests: 360/360.
- AI chronicle storage/translation: V6 `match_chronicles` (one row per fixture+language, FK ON DELETE CASCADE), `MatchChronicle`/`ChronicleRepository`, `ChroniclePort.translate`. `GET /api/fixtures/{id}/chronicle?lang=` returns the stored text or translates the other-language one (204 if none), `POST` writes a new one and discards older versions. Page load calls GET, so switching EN/HR translates instead of regenerating. Recording/correcting a result or adding an event deletes stored chronicles (stale). AI failures now throw `ChronicleUnavailableException` → REST 503 `AI Provider Unavailable` (nothing is stored; no fake fallback text). Tests: 370/370.
- Event deletion & score-mismatch warning (`6188844`, `dc19eca`, `e2223ac`):
  - Domain/Service: `FixtureService.deleteMatchEvent(fixtureId, eventId)` removes the event and deletes stale chronicles for the fixture (they described the removed event). `MatchEventResponse` now exposes `id`.
  - REST API: `DELETE /api/fixtures/{id}/events/{eventId}` (204, restricted to `ADMIN` and `OFFICIAL`).
  - Web UI: in the Actions tab under "Record Match Event", all recorded events are listed with a red trash button (with confirmation prompt). Flash message `fixtures.flash.eventDeleted`.
  - Score-mismatch guard: an amber alert banner appears in the Actions tab of finished matches whenever total recorded goal events (`GOAL`, `PENALTY`, `OWN_GOAL`) do not equal `homeScore + awayScore`. Non-blocking informative warning. Fully translated (EN + HR).
  - Test suite: 374/374 passing (`BUILD SUCCESS`).
- Automatic score synchronization from match events:
  - Domain model: `Fixture.java` automatically updates `homeScore` and `awayScore` whenever goal events (`GOAL`, `PENALTY`, `OWN_GOAL`) are added via `addMatchEvent` or removed via `removeMatchEvent`.
  - Own goals: an `OWN_GOAL` attributed to `homeTeam` increments `awayGoals`, and vice-versa.
  - Non-goal events (`YELLOW_CARD`, `RED_CARD`, `SUBSTITUTION`) never alter the score. Fixtures with zero goal events keep the score entered by "Record result". Goal events are locked once the fixture is FINISHED (see section 8).
  - Aggregate test updated in `DomainModelTest.java` (3 goal events added to match final 2-1 score).
  - Mockito unit tests in `FixtureServiceTest.java`: 2 new tests covering own goals and non-goal events, plus score assertions on addition and deletion. Total tests: 376/376.
  - Live browser smoke verified: adding an away goal immediately updated the score header from 3-1 to 3-2 without warning banner; deleting it immediately dropped the score back to 3-1.
- Administrative correction framing (`a0a6947`):
  - Retained `ResultAudit` and `Correct Result` in Actions tab to preserve Course P39 LO4 audit rubric compliance.
  - Rebranded card to "Administrative Result Correction" / "Administrativni ispravak rezultata" with explanatory subtitle clarifying that standard goals are managed via match events while this form is for official audited score overrides.
  - Test suite: 376/376 passing (`BUILD SUCCESS`).
- Real role accounts (`6e3cc69`, `e8846ab`): OFFICIAL and VIEWER logins are created at startup from `OFFICIAL_USERNAME/PASSWORD` and `VIEWER_USERNAME/PASSWORD` in `.env` (same mechanism as the admin; `InitialUserSeeder` now takes 8 env values, skips unset roles, fails fast if a username is set without a password). The "View as" switcher is gone: `SwitchUserFilter`, `/dev/switch*`, `ROLE_SWITCH_USER`, JS/CSS and `common.viewas`. V7 migration deletes `ROLE_SWITCH_USER` and the old `test-official`/`test-viewer` accounts. Topbar always shows the role pill. Verified live: official/viewer/admin logins, old `test-*` login rejected, DB has exactly 3 users. Tests: 375/375.
- Mobile layout: below 768 px the nav links move to a bottom tab bar (`.app-tabbar` in `layout.html`, Dashboard/Leagues/Teams, same keys as the topbar) and the topbar keeps brand + EN/HR + Sign out; below 576 px the standings table hides GF/GA and keeps team names on one line (ellipsis). Dead sidebar JS removed. Desktop unchanged. Verified at 390 px (EN/HR) and 1440 px. Tests: 375/375.

## 8. Session 2026-11-08 — security audit fixes
- **Finding 1 (goal events vs. audit)**: goal events (`GOAL`, `PENALTY`, `OWN_GOAL`) can be added or removed only while the fixture is `SCHEDULED` or `IN_PROGRESS`; on `FINISHED`/`POSTPONED`/`CANCELLED` `Fixture` throws `InvalidStateTransitionException` (`addGoalEvent`/`removeGoalEvent`; views show it as a flash error). The only way to change a finished score is `correctResult()` (writes a `ResultAudit`). Non-goal events (cards, substitutions) stay allowed in every status and never touch the score. `Fixture.addMatchEvent` also rejects (`IllegalArgumentException` → 400) an event whose team is neither home nor away; `syncScoreFromEvents()` is now private. UI: the event card is shown for `SCHEDULED`/`IN_PROGRESS`/`FINISHED`; on `FINISHED` goal types are not offered and goal rows show a lock instead of the delete button. Workflow consequence: goals are recorded before "Record result" (nothing in the domain moves a fixture to `IN_PROGRESS` yet); afterwards fix mistakes with the administrative correction. Tests: 388/388.
- **Finding 2 (chronicle cost/access)**: `POST /api/fixtures/{id}/chronicle` and the new `POST .../chronicle/translate` are `ADMIN`/`OFFICIAL` only (AGENTS.md §5: VIEWER is read-only). `GET .../chronicle` stays open to every role but is now strictly read-only: it returns the stored text of the requested language or 204 and never calls the provider (decision: no automatic translation for VIEWER; staff translate via the explicit endpoint, which the fixture page calls on load when only the other language is stored). `ChronicleService` is no longer transactional: it runs `ChronicleStore` (new bean, short transactions: read/build request → provider call outside any transaction → delete+save) so no pooled DB connection is held during the AI call; nothing is deleted or stored when the provider fails, and a translation whose source was regenerated meanwhile is discarded. Spring AI 2.0.2 has no timeout/retry property for Google GenAI, so `ChronicleAdapterConfig` defines the `com.google.genai.Client` with `HttpOptions.timeout(app.ai.timeout-ms)` (30000 ms per attempt) and `HttpRetryOptions.attempts(app.ai.max-attempts)` (2 total attempts), both in `application.yml`; measured with a 1 ms timeout: the SDK's default retry took ~14 s to fail, the capped client 2 s, both ending in 503 with the stored chronicle untouched. UI: VIEWER sees the stored chronicle and a hint, no Generate button, and `app.js` never POSTs for VIEWER. Not done: per-user rate limiting (staff can still trigger paid calls; they are trusted roles).
- **Finding 3 (DB credential on a public port)**: `docker-compose.yml` now publishes Postgres on `127.0.0.1:5432` only and takes the credentials from `.env` (`SPRING_DATASOURCE_PASSWORD` is mandatory — compose refuses to start without it; user and db have non-secret defaults). The `dev` profile in `application.yml` reads `SPRING_DATASOURCE_URL`/`USERNAME` (defaults: local URL, `league_admin`) and `SPRING_DATASOURCE_PASSWORD` (no default, the app fails fast without it). The `test` profile (H2) and the Testcontainers profile do not use these values. README documents the setup and the volume caveat (an existing volume keeps the OLD password: `docker compose down -v` or `\password league_admin` in psql). **The value `league_dev_secret` was committed and published in git history and must be considered burned: rotate the password of any real database that ever used it** (history was not rewritten).
