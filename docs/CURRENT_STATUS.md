# Current Project Status — Sports League Management

**Last Updated**: 2026-11-14
**Active Branch**: `main`
**Latest Local Commit**: `7584c29 docs(status): record usability feedback session` (+ the `docs(status)` commit that records the 2026-11-14 audit)
**Git Working Tree**: Clean after that commit.
**Push to origin**: `main` equals `origin/main` as of 2026-11-14. Any further push still needs explicit user authorization per AGENTS.md §6.4.

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

## 4. Test Suite — 548/548

```
cmd.exe /c "cd /d C:\Users\Bienve\dev\Java\SportsLeagueManagement && mvnw.cmd verify"
Tests run: 548, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS, JaCoCo "All coverage checks have been met" (2026-11-10)
```

The per-class table below is the 2026-11-07 snapshot; new classes since then: `TeamSquadTest`, `FixtureLineupTest`, `LineupRulesTest`, `PitchTimelineTest`, `SuspensionCalculatorTest`, `PlayerStatsCalculatorTest`, `FixtureViewControllerTest`.

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
- **Verification (2026-11-08)**: full suite 401/401 (`BUILD SUCCESS`). Runtime smoke (app on Postgres with the `.env` credentials, `docker compose ps` → `127.0.0.1:5432->5432/tcp`, data volume intact): OFFICIAL forged goal on a FINISHED fixture → flash "Cannot addGoalEvent Fixture in state FINISHED", score 2-1 and events unchanged; event of a foreign team → 400; VIEWER POST generate/translate → 403, no Generate button, GET of a missing language → 204 with a hint, stored text readable; OFFICIAL generate (EN, 12 s) and automatic HR translation (8 s) against real Gemini; `docker compose --env-file <empty> config` fails with "set SPRING_DATASOURCE_PASSWORD in .env".
- **Known issues / follow-ups**: (1) nothing moves a fixture to `IN_PROGRESS`, so the lifecycle is SCHEDULED → FINISHED; goals must be entered before "Record result". (2) A chronicle generated while another request changes the fixture can still describe the older state (same window as before, now without a held DB connection). (3) Two concurrent "Generate" requests for the same fixture can hit the unique `(fixture_id, language)` constraint (500); staff-only, rare. (4) No per-user rate limit on paid AI calls. (5) An existing Postgres volume keeps its old password: align it with `.env` (README).

## 9. Session 2026-11-10 — match sheet features (squads, lineups, live match, substitutions, suspensions, player stats)
Six commits, each with a green full suite: `1554fd7`, `cb9f072`, `57533bb`, `0e2d7ab`, `947a5c5`, `287df81`.
- **Squads** (`1554fd7`): V8 `players` (team FK, shirt number 1–99, free-text `playing_position`, `active`). `Player` is managed through `Team.addPlayer/updatePlayer/removePlayer`; shirt numbers are unique among active players (domain rule, no DB constraint); removal only deactivates, so history keeps its references. `TeamService` squad methods, REST `GET/POST /api/teams/{id}/players`, `PUT/DELETE .../players/{playerId}` (mutations ADMIN), Squad card on the team page + `teams/player-form.html`.
- **Live match** (`cb9f072`): `Fixture.kickOff()` (SCHEDULED → IN_PROGRESS, 0-0) and `finish()` (IN_PROGRESS → FINISHED, discards stale chronicles). No event before kick-off; goal events only while IN_PROGRESS. "Record result" straight from SCHEDULED still works. REST `POST /api/fixtures/{id}/kick-off|finish`, "Match control" card. Closes known issue (1) of section 8.
- **Lineups** (`57533bb`): V9 `lineup_entries` (unique fixture+player) and `match_events.player_id/related_player_id`. `LineupRules` enum holds per-sport sizes (football 11 + 12, basketball 5 + 7, handball 7 + 9, volleyball 6 + 8). `Fixture.submitLineup/clearLineup` only before kick-off, applied as a diff (avoids the insert-before-delete unique violation). Flexible sheet: kick-off without lineups is allowed with a warning + confirm. A team with a lineup must reference lineup players in its events; without a lineup free-text names still work. `relatedPlayer` = assist provider, or player coming on in a SUBSTITUTION. New "Lineups" tab (all roles) and lineup editor (ADMIN/OFFICIAL); event form switches between lineup selects and free text (`app.js`). REST `GET/PUT/DELETE /api/fixtures/{id}/lineups[/{teamId}]`. `RecordMatchEventRequest` gained `playerId`, `relatedPlayerId`; `playerName` no longer `@NotBlank`.
- **Substitutions** (`0e2d7ab`): `PitchTimeline` replays starters + substitutions + red cards by minute: player off must be on the pitch, player on must not be, no re-entry in football, sent-off players cannot come on, limit per sport (football 5). An impossible event is rejected and not stored; deleting an event that later events depend on is refused.
- **Suspensions** (`947a5c5`): `SuspensionCalculator` derives bans from the team's last finished match of the season: red card, or every 5th yellow card → 1 match. Enforced in `submitLineup` and again at kick-off; the lineup editor disables suspended players and the Lineups tab lists them.
- **Player stats** (`287df81`): `PlayerStatsCalculator` (finished fixtures only): appearances, minutes (from lineups; none for volleyball), goals, assists, cards. Legacy text-only events are grouped per team and name. Page `/leagues/{l}/seasons/{s}/stats` ("Player stats" button), REST `GET .../player-stats`.
- **Verification (2026-11-10)**: `mvnw verify` → 539/539, JaCoCo gate met. Runtime smoke on PostgreSQL 17 (Flyway applied V8 + V9 to the existing dev DB): ADMIN added 14 players to Dinamo and Hajduk, duplicate #10 → "Shirt number 10 is already taken by Marko Pjaca", edit + removal OK. OFFICIAL on a new Dinamo–Hajduk fixture: 10 starters → "Exactly 11 starters are required, got 10"; 11 + 3 + captain saved; one-lineup warning "No lineup submitted for: HNK Hajduk Split" with kick-off confirm; kick-off → In progress 0-0, editor hidden. Goal from dropdown → 1-0 with assist; sub 60' accepted, "(on: …)" shown; same player on again at 70' → "Sandro Kulenovic is already on the pitch at minute 70"; second red → "already been sent off". Final whistle. Next Dinamo fixture: red-carded #4 disabled with "red card" badge and listed as suspended; forged POST with #4 → "Sergi Dominguez is suspended for this match". Five subs accepted, sixth → "Substitution limit reached (5)". Rijeka (no lineup) free-text goal still accepted. Stats page as VIEWER: Petkovic #9 150' 1 goal, replaced starter 60' + 55' = 115', Dominguez 75' 1 red, seeded text scorers listed without shirt number. VIEWER: Lineups tab visible, no Actions tab, POST clear lineup → 403. HR labels verified (tabs, lineups, "(ulazi: …)", stats headers, "Momčad"). The dev DB now holds these smoke-test players, rounds 2–3 and their fixtures.
- **Known issues / follow-ups**: (1) ~~Legacy text rows duplicated in stats~~ fixed in section 10. (2) A second yellow card in one match is not turned into a red automatically; the official records the red card. (3) Suspension rule (red = 1 match, every 5th yellow = 1 match) is the same for every sport. (4) Assist providers named only as free text get no appearance.

## 10. Session 2026-11-10 (b) — usability feedback
Three commits: `879a5bc`, `4ca942b`, `0268571`.
- **Navigation** (`879a5bc`): the fixture page "Back to calendar" was `history.back()`, so after a form submit or a detour to a team page it went to the wrong page. It now links to the season calendar at the match's round (`#round-{id}` anchors; `FixtureResponse` gained `seasonId`, `leagueId`). `app.js` remembers the active tab per page and restores it when the previous page was the same page (every POST-redirect, error flashes included), so saving a lineup or an event stays on Actions; a URL hash (`#actions`, `#lineups`) also selects a tab. "Manage squad" opens the team page with `?fixture=`, whose back arrow then returns to the match.
- **Record match event**: the goal types were hidden on FINISHED matches (goals are locked after the final whistle, section 8) and the card was hidden before kick-off. Now all six types are always listed — Goal, Penalty, Own goal, Yellow, Red, Substitution — with goal types disabled and marked "(locked)" after the final whistle; before kick-off the card explains that "Kick off" must be pressed first.
- **Squads** (`4ca942b`): V10 seeds an 18-player squad for each seeded team (Dinamo, Hajduk, Rijeka, Osijek) only when the team has no players, so lineups are picked by clicking. Team page has "Add several players": one per line `<number> <name>, <position>`, all-or-nothing, error names the line and keeps the pasted text (`TeamService.addPlayersFromText`).
- **Player stats** (`0268571`): five leaderboards side by side (top scorers, top assists, goals + assists, most minutes — or appearances for volleyball —, discipline) above the full table; `PlayerStatsResponse` carries them (`PlayerStatsCalculator.leaders`). Free-text names of legacy events are now credited to the squad player of the same team with the same name.
- **Verification**: `mvnw verify` 548/548, JaCoCo met. Runtime on PostgreSQL (port 8081, the user's own app was on 8080): V10 applied (Rijeka/Osijek 18 players, Dinamo kept its 20); bulk paste with a taken number → "Line 2: Shirt number 8 is already taken by Toni Fruk", nothing added, text kept; valid paste added 2. New Rijeka–Osijek fixture: "not started" notice, 18 clickable rows, lineup saved by clicks and Actions tab kept; back link → calendar `#round-…`; team page with `?fixture=` back → match. IN_PROGRESS lists all six types; finished derby shows goal types "(locked)". Stats page shows the five boards; Petkovic now 2 goals (seeded text goal merged).

## 11. Session 2026-11-14 — audit against the professor's assignment (no code changes)
Source: `Student_Project_Assignments_2026-27.docx` (topic P39, seven learning outcomes, section 3 artefacts, section 9 rubric: at least 50 % of the points of **every** LO are needed to pass). Verified in code, not in documents.
- **Verified**: `mvnw verify` → 548/548, 0 failures, JaCoCo gate met (`application.service` 93.8 %, total 76.9 %). `main` equals `origin/main`.
- **Met**: layering and constructor injection; three roles with BCrypt, CSRF and the Thymeleaf security dialect; PostgreSQL + Flyway V1–V10 with `ddl-auto=validate`; JPA one-to-many, association entity (`SeasonTeam`), optional relation (`MatchEvent.player`), lazy fetching with `JOIN FETCH`/`@BatchSize`; REST with DTOs, ProblemDetail, springdoc 3.1.1; AI behind a port with a stub; EN/HR UI with a persistent cookie switcher.
- **Missing or partial — LO7**: no scheduled job at all (neither Quartz nor `@Scheduled`); Bean Validation messages are hard-coded English strings in the DTOs; dates/numbers are not formatted per language.
- **Missing or partial — delivery artefacts**: no `Dockerfile` (compose only runs the DB), no `.github` CI or badge, no deployment (no actuator, no `PORT`), no Figma/`docs/prototype.pdf`, no `docs/ai-usage.md`, no `docs/ai-evaluation.md`, only two ADRs (persistence, value objects; the AI ADR is missing), no measured performance improvement, N+1 fix not documented with before/after SQL, README lacks live URL, demo credentials, limitations, next steps, Figma mapping, badge.
- **Missing or partial — other LOs**: REST lists are not paginated/sortable and there is no 422 (LO5); `@PreAuthorize` is only on controllers, not on service methods (LO2); no CRUD edit for Season/Round/Fixture (LO1); JaCoCo report not committed (LO6); pool choice not justified in prose (LO3).
- **AI feature vs. spec**: the report is built from teams, score and events, not from the standings effect; nothing checks that the text contains the right scorelines; the text is stored without a review step (the rubric deducts for unreviewed AI output in the domain).
- **Defects found**: (1) `mvnw verify -Pintegration` runs **0 tests**: the build-level `excludedGroups=integration` is inherited and cancels the profile's `groups=integration`. With the exclusion removed the 3 Testcontainers tests start and error only because Docker Desktop was stopped, so there is no evidence yet that they pass. (2) Tie-breaks are hard-coded (points, goal difference, goals for, head-to-head points, head-to-head goal difference), not configurable, and the README lists a different order. (3) No test covers "correct a result weeks later and the table order changes" (the topic's hard part). (4) `docs/ARCHITECTURE.md` and `docs/uml/04-infrastructure.puml` are stale.
- **Suggested order**: deployment (Dockerfile, actuator, prod profile, Render + Neon) + CI + scheduled jobs; i18n of validation/dates; AI documents and ADR; Figma; performance numbers; then pagination/422, service-level security, the integration-profile fix and the hard-part test. Full roadmap in the Obsidian project note (`Brain/01 Projects/Sports League Management.md`, section 12).
