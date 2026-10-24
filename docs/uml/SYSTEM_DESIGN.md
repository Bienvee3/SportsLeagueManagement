# System Design — Sports League Management

> **Generated from source code** on 2026-10-24.
> Documents only what is implemented and verified; no planned/future components.

---

## 1. Component Diagram

```mermaid
flowchart TB
    subgraph TestLayer["🧪 Test Layer"]
        DMT["DomainModelTest\n(unit · JUnit 6)"]
        CTX["SportsLeagueManagement\nApplicationTests\n(integration · @SpringBootTest)"]
    end

    subgraph DomainLayer["🏛️ Domain Layer — com.league.sportsleague.domain"]
        direction LR
        subgraph Entities["JPA Entities"]
            League
            Season
            Team
            SeasonTeam
            Round
            Fixture
            MatchEvent
            ResultAudit
        end
        subgraph Enums["Status & Type Enums"]
            SeasonStatus
            SeasonTeamStatus
            RoundStatus
            FixtureStatus
            MatchEventType
        end
    end

    subgraph InfraLayer["⚙️ Persistence / Infrastructure Layer"]
        direction LR
        FW["Flyway Migrations\nV1__init_schema.sql\nV2__seed_initial_data.sql"]
        HIB["Hibernate 7.4.5\nJPA Entity Mappings\nFetchType.LAZY"]
        HK["HikariCP\nConnection Pool"]
        PG["PostgreSQL 17\n(Docker · dev/prod)"]
        H2["H2 In-Memory\n(test profile · PG compat mode)"]
    end

    DMT -- "constructs & asserts\n(pure unit)" --> DomainLayer
    CTX -- "@ActiveProfiles(test)\nloads Spring context" --> DomainLayer
    CTX -- "context loads\nvalidates schema" --> InfraLayer

    DomainLayer -- "JPA annotations\n@Entity / @Table / @Column" --> HIB
    HIB -- "schema validate\nddl-auto=validate" --> FW
    FW -- "DDL + seed SQL" --> PG
    FW -- "DDL + seed SQL\n(PG compat mode)" --> H2
    HK -- "pooled connections\n(dev: max 10, prod: max 5)" --> PG
```

---

## 2. Domain Class Diagram

```mermaid
classDiagram
    direction TB

    class League {
        -UUID id
        -String name
        -String slug  ‹‹business key››
        -String sportType = "FOOTBALL"
        -String country
        -Instant createdAt
        -Instant updatedAt
        -List~Season~ seasons
        +League()
        +League(name, slug, sportType, country)
        +updateDetails(name, country, sportType) void
        +addSeason(Season) void
        +removeSeason(Season) void
        +getSeasons() List~Season~ «unmodifiable»
        +equals/hashCode → slug
    }

    class Season {
        -UUID id
        -League league
        -String name
        -int yearStart
        -int yearEnd
        -SeasonStatus status = UPCOMING
        -Instant createdAt
        -List~SeasonTeam~ seasonTeams
        -List~Round~ rounds
        +Season()
        +Season(league, name, yearStart, yearEnd, status)
        +activate() void
        +complete() void
        +updateStatus(SeasonStatus) void
        +addSeasonTeam(SeasonTeam) void
        +removeSeasonTeam(SeasonTeam) void
        +addRound(Round) void
        +removeRound(Round) void
        +getSeasonTeams() List~SeasonTeam~ «unmodifiable»
        +getRounds() List~Round~ «unmodifiable»
        +equals/hashCode → league.slug + name
    }

    class Team {
        -UUID id
        -String name
        -String shortCode  ‹‹business key››
        -String stadiumName
        -String city
        -Integer foundedYear
        -Instant createdAt
        -List~SeasonTeam~ seasonTeams
        +Team()
        +Team(name, shortCode, stadiumName, city, foundedYear)
        +updateDetails(name, stadiumName, city, foundedYear) void
        +getSeasonTeams() List~SeasonTeam~ «unmodifiable»
        +equals/hashCode → shortCode
    }

    class SeasonTeam {
        -UUID id
        -Season season
        -Team team
        -int initialSeed = 0
        -Instant registrationDate
        -SeasonTeamStatus status = CONFIRMED
        +SeasonTeam()
        +SeasonTeam(season, team, initialSeed, status)
        +updateSeed(int) void
        +updateStatus(SeasonTeamStatus) void
        +withdraw() void
        +disqualify() void
        +equals/hashCode → season.id + team.id
    }

    class Round {
        -UUID id
        -Season season
        -int roundNumber
        -String name
        -LocalDate startDate
        -LocalDate endDate
        -RoundStatus status = SCHEDULED
        -List~Fixture~ fixtures
        +Round()
        +Round(season, roundNumber, name, startDate, endDate, status)
        +startRound() void
        +completeRound() void
        +updateSchedule(startDate, endDate) void
        +addFixture(Fixture) void
        +removeFixture(Fixture) void
        +getFixtures() List~Fixture~ «unmodifiable»
        +equals/hashCode → season.id + roundNumber
    }

    class Fixture {
        -UUID id
        -Round round
        -Team homeTeam
        -Team awayTeam
        -Integer homeScore
        -Integer awayScore
        -FixtureStatus status = SCHEDULED
        -Instant scheduledTime
        -String postponementReason
        -List~MatchEvent~ events
        -List~ResultAudit~ audits
        +Fixture()
        +Fixture(round, homeTeam, awayTeam, scheduledTime)
        +recordResult(homeScore, awayScore) void
        +postpone(reason) void
        +cancel() void
        +reschedule(newTime) void
        +addMatchEvent(MatchEvent) void
        +removeMatchEvent(MatchEvent) void
        +addResultAudit(ResultAudit) void
        +getEvents() List~MatchEvent~ «unmodifiable»
        +getAudits() List~ResultAudit~ «unmodifiable»
        +equals/hashCode → round.id + homeTeam.shortCode + awayTeam.shortCode
    }

    class MatchEvent {
        -UUID id
        -Fixture fixture
        -Team team
        -MatchEventType eventType
        -int matchMinute
        -String playerName
        -String assistingPlayer
        -String notes
        -Instant createdAt
        +MatchEvent()
        +MatchEvent(fixture, team, eventType, matchMinute, playerName)
        +MatchEvent(fixture, team, eventType, matchMinute, playerName, assistingPlayer, notes)
        +equals/hashCode → fixture.id + team.id + eventType + matchMinute + playerName
    }

    class ResultAudit {
        -UUID id
        -Fixture fixture
        -Integer oldHomeScore
        -Integer oldAwayScore
        -int newHomeScore
        -int newAwayScore
        -String reason
        -String correctedBy
        -Instant correctedAt
        +ResultAudit()
        +ResultAudit(fixture, oldHomeScore, oldAwayScore, newHomeScore, newAwayScore, reason, correctedBy)
        +equals/hashCode → id  «immutable audit record»
    }

    class SeasonStatus {
        <<enumeration>>
        UPCOMING
        ACTIVE
        COMPLETED
    }

    class SeasonTeamStatus {
        <<enumeration>>
        CONFIRMED
        WITHDRAWN
        DISQUALIFIED
    }

    class RoundStatus {
        <<enumeration>>
        SCHEDULED
        IN_PROGRESS
        COMPLETED
    }

    class FixtureStatus {
        <<enumeration>>
        SCHEDULED
        IN_PROGRESS
        FINISHED
        POSTPONED
        CANCELLED
    }

    class MatchEventType {
        <<enumeration>>
        GOAL
        PENALTY
        YELLOW_CARD
        RED_CARD
        OWN_GOAL
        SUBSTITUTION
    }

    League "1" --> "0..*" Season : seasons\n(cascade ALL, orphanRemoval)
    Season "0..*" --> "1" League : league\n(LAZY, FK league_id)

    Season "1" --> "0..*" SeasonTeam : seasonTeams\n(cascade ALL, orphanRemoval)
    Season "1" --> "0..*" Round : rounds\n(cascade ALL, orphanRemoval)

    Team "1" --> "0..*" SeasonTeam : seasonTeams\n(cascade ALL, orphanRemoval)

    SeasonTeam "0..*" --> "1" Season : season\n(LAZY, FK season_id)
    SeasonTeam "0..*" --> "1" Team : team\n(LAZY, FK team_id)

    Round "0..*" --> "1" Season : season\n(LAZY, FK season_id)
    Round "1" --> "0..*" Fixture : fixtures\n(cascade ALL, orphanRemoval)

    Fixture "0..*" --> "1" Round : round\n(LAZY, FK round_id)
    Fixture "0..*" --> "1" Team : homeTeam\n(LAZY, FK home_team_id)
    Fixture "0..*" --> "1" Team : awayTeam\n(LAZY, FK away_team_id)
    Fixture "1" --> "0..*" MatchEvent : events\n(cascade ALL, orphanRemoval)
    Fixture "1" --> "0..*" ResultAudit : audits\n(cascade ALL, orphanRemoval)

    MatchEvent "0..*" --> "1" Fixture : fixture\n(LAZY, FK fixture_id)
    MatchEvent "0..*" --> "1" Team : team\n(LAZY, FK team_id)

    ResultAudit "0..*" --> "1" Fixture : fixture\n(LAZY, FK fixture_id)

    Season ..> SeasonStatus : uses
    SeasonTeam ..> SeasonTeamStatus : uses
    Round ..> RoundStatus : uses
    Fixture ..> FixtureStatus : uses
    MatchEvent ..> MatchEventType : uses
```

---

## 3. Entity-Relationship Diagram (ERD)

Reflects the exact `V1__init_schema.sql` DDL.

```mermaid
erDiagram
    leagues {
        UUID id PK
        VARCHAR_100 name "NOT NULL"
        VARCHAR_100 slug "NOT NULL, UNIQUE"
        VARCHAR_50 sport_type "NOT NULL, DEFAULT 'FOOTBALL'"
        VARCHAR_100 country "NOT NULL"
        TIMESTAMPTZ created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
        TIMESTAMPTZ updated_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    teams {
        UUID id PK
        VARCHAR_100 name "NOT NULL"
        VARCHAR_10 short_code "NOT NULL, UNIQUE"
        VARCHAR_100 stadium_name "NOT NULL"
        VARCHAR_100 city "NOT NULL"
        INTEGER founded_year "NULLABLE"
        TIMESTAMPTZ created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    seasons {
        UUID id PK
        UUID league_id FK "NOT NULL, ON DELETE CASCADE"
        VARCHAR_100 name "NOT NULL"
        INTEGER year_start "NOT NULL"
        INTEGER year_end "NOT NULL"
        VARCHAR_20 status "NOT NULL, DEFAULT 'UPCOMING'"
        TIMESTAMPTZ created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    season_teams {
        UUID id PK
        UUID season_id FK "NOT NULL, ON DELETE CASCADE"
        UUID team_id FK "NOT NULL, ON DELETE CASCADE"
        INTEGER initial_seed "NOT NULL, DEFAULT 0"
        TIMESTAMPTZ registration_date "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
        VARCHAR_20 status "NOT NULL, DEFAULT 'CONFIRMED'"
    }

    rounds {
        UUID id PK
        UUID season_id FK "NOT NULL, ON DELETE CASCADE"
        INTEGER round_number "NOT NULL"
        VARCHAR_100 name "NOT NULL"
        DATE start_date "NULLABLE"
        DATE end_date "NULLABLE"
        VARCHAR_20 status "NOT NULL, DEFAULT 'SCHEDULED'"
    }

    fixtures {
        UUID id PK
        UUID round_id FK "NOT NULL, ON DELETE CASCADE"
        UUID home_team_id FK "NOT NULL"
        UUID away_team_id FK "NOT NULL"
        INTEGER home_score "NULLABLE"
        INTEGER away_score "NULLABLE"
        VARCHAR_20 status "NOT NULL, DEFAULT 'SCHEDULED'"
        TIMESTAMPTZ scheduled_time "NOT NULL"
        VARCHAR_255 postponement_reason "NULLABLE"
    }

    match_events {
        UUID id PK
        UUID fixture_id FK "NOT NULL, ON DELETE CASCADE"
        UUID team_id FK "NOT NULL"
        VARCHAR_30 event_type "NOT NULL"
        INTEGER match_minute "NOT NULL, CHECK 1..130"
        VARCHAR_100 player_name "NOT NULL"
        VARCHAR_100 assisting_player "NULLABLE"
        VARCHAR_255 notes "NULLABLE"
        TIMESTAMPTZ created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    result_audits {
        UUID id PK
        UUID fixture_id FK "NOT NULL, ON DELETE CASCADE"
        INTEGER old_home_score "NULLABLE"
        INTEGER old_away_score "NULLABLE"
        INTEGER new_home_score "NOT NULL"
        INTEGER new_away_score "NOT NULL"
        TEXT reason "NOT NULL"
        VARCHAR_100 corrected_by "NOT NULL"
        TIMESTAMPTZ corrected_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    leagues ||--o{ seasons : "has"
    seasons ||--o{ season_teams : "enrolls"
    teams ||--o{ season_teams : "participates"
    seasons ||--o{ rounds : "contains"
    rounds ||--o{ fixtures : "schedules"
    teams ||--o{ fixtures : "plays (home)"
    teams ||--o{ fixtures : "plays (away)"
    fixtures ||--o{ match_events : "records"
    teams ||--o{ match_events : "involved"
    fixtures ||--o{ result_audits : "audited by"
```

### Constraints & Indexes (from V1 DDL)

| Table | Constraint / Index | Type | Definition |
|---|---|---|---|
| `leagues` | `slug` | UNIQUE | Natural business key |
| `seasons` | `uk_seasons_league_name` | UNIQUE | `(league_id, name)` |
| `seasons` | `idx_seasons_league_id` | INDEX | `league_id` |
| `season_teams` | `uk_season_teams_season_team` | UNIQUE | `(season_id, team_id)` |
| `season_teams` | `idx_season_teams_season_id` | INDEX | `season_id` |
| `season_teams` | `idx_season_teams_team_id` | INDEX | `team_id` |
| `rounds` | `uk_rounds_season_round_number` | UNIQUE | `(season_id, round_number)` |
| `rounds` | `idx_rounds_season_id` | INDEX | `season_id` |
| `fixtures` | `chk_fixtures_different_teams` | CHECK | `home_team_id <> away_team_id` |
| `fixtures` | `idx_fixtures_round_id` | INDEX | `round_id` |
| `fixtures` | `idx_fixtures_home_team_id` | INDEX | `home_team_id` |
| `fixtures` | `idx_fixtures_away_team_id` | INDEX | `away_team_id` |
| `match_events` | `chk_match_events_minute` | CHECK | `match_minute >= 1 AND match_minute <= 130` |
| `match_events` | `idx_match_events_fixture_id` | INDEX | `fixture_id` |
| `match_events` | `idx_match_events_team_id` | INDEX | `team_id` |
| `result_audits` | `idx_result_audits_fixture_id` | INDEX | `fixture_id` |

---

## 4. Aggregate Ownership Diagram

```mermaid
flowchart LR
    subgraph AggregateRoot["Aggregate Root"]
        L["League"]
    end

    subgraph SeasonAggregate["Season Aggregate"]
        S["Season"]
        ST["SeasonTeam\n(association entity)"]
    end

    subgraph RoundAggregate["Round Aggregate"]
        R["Round"]
    end

    subgraph FixtureAggregate["Fixture Aggregate"]
        F["Fixture"]
        ME["MatchEvent"]
        RA["ResultAudit"]
    end

    T["Team\n(independent entity)"]

    L -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> S
    S -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> ST
    S -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> R
    R -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> F
    F -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> ME
    F -- "cascade ALL\norphanRemoval\n1 ➜ 0..*" --> RA

    ST -. "FK team_id\n(no cascade from Team)" .-> T
    F -. "FK home_team_id\n(no cascade)" .-> T
    F -. "FK away_team_id\n(no cascade)" .-> T
    ME -. "FK team_id\n(no cascade)" .-> T

    style L fill:#4e79a7,color:#fff,stroke:#2d5f8a
    style S fill:#59a14f,color:#fff,stroke:#3d7a36
    style R fill:#f28e2b,color:#fff,stroke:#c47020
    style F fill:#e15759,color:#fff,stroke:#b34345
    style ME fill:#edc948,color:#000,stroke:#c4a63a
    style RA fill:#b07aa1,color:#fff,stroke:#8c5f82
    style T fill:#76b7b2,color:#000,stroke:#5a9490
    style ST fill:#ff9da7,color:#000,stroke:#d98089
```

### Cascade Chain Summary

| Parent | Child | Cascade | Orphan Removal | FK ON DELETE |
|---|---|---|---|---|
| League | Season | ALL | ✅ | CASCADE |
| Season | SeasonTeam | ALL | ✅ | CASCADE |
| Season | Round | ALL | ✅ | CASCADE |
| Round | Fixture | ALL | ✅ | CASCADE |
| Fixture | MatchEvent | ALL | ✅ | CASCADE |
| Fixture | ResultAudit | ALL | ✅ | CASCADE |
| Team | SeasonTeam | ALL | ✅ | CASCADE |

---

## 5. Enum State Diagrams

### 5.1 SeasonStatus

Transitions derived from `Season.activate()`, `Season.complete()`, and `Season.updateStatus()`.

```mermaid
stateDiagram-v2
    [*] --> UPCOMING : constructor default
    UPCOMING --> ACTIVE : activate()
    ACTIVE --> COMPLETED : complete()
    UPCOMING --> ACTIVE : updateStatus(ACTIVE)
    UPCOMING --> COMPLETED : updateStatus(COMPLETED)
    ACTIVE --> UPCOMING : updateStatus(UPCOMING)
    ACTIVE --> COMPLETED : updateStatus(COMPLETED)
    COMPLETED --> UPCOMING : updateStatus(UPCOMING)
    COMPLETED --> ACTIVE : updateStatus(ACTIVE)
```

### 5.2 RoundStatus

Transitions derived from `Round.startRound()` and `Round.completeRound()`.

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED : constructor default
    SCHEDULED --> IN_PROGRESS : startRound()
    IN_PROGRESS --> COMPLETED : completeRound()
```

### 5.3 FixtureStatus

Transitions derived from `Fixture.recordResult()`, `Fixture.postpone()`, `Fixture.cancel()`, and `Fixture.reschedule()`.

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED : constructor default
    SCHEDULED --> FINISHED : recordResult(home, away)
    SCHEDULED --> POSTPONED : postpone(reason)
    SCHEDULED --> CANCELLED : cancel()
    IN_PROGRESS --> FINISHED : recordResult(home, away)
    IN_PROGRESS --> POSTPONED : postpone(reason)
    IN_PROGRESS --> CANCELLED : cancel()
    POSTPONED --> SCHEDULED : reschedule(newTime)
    POSTPONED --> FINISHED : recordResult(home, away)
    POSTPONED --> CANCELLED : cancel()
```

### 5.4 SeasonTeamStatus

Transitions derived from `SeasonTeam.withdraw()`, `SeasonTeam.disqualify()`, and `SeasonTeam.updateStatus()`.

```mermaid
stateDiagram-v2
    [*] --> CONFIRMED : constructor default
    CONFIRMED --> WITHDRAWN : withdraw()
    CONFIRMED --> DISQUALIFIED : disqualify()
    CONFIRMED --> WITHDRAWN : updateStatus(WITHDRAWN)
    CONFIRMED --> DISQUALIFIED : updateStatus(DISQUALIFIED)
    WITHDRAWN --> CONFIRMED : updateStatus(CONFIRMED)
    WITHDRAWN --> DISQUALIFIED : updateStatus(DISQUALIFIED)
    DISQUALIFIED --> CONFIRMED : updateStatus(CONFIRMED)
    DISQUALIFIED --> WITHDRAWN : updateStatus(WITHDRAWN)
```

### 5.5 MatchEventType

`MatchEventType` is a classification enum with no state transitions. Values:

| Constant | Description |
|---|---|
| `GOAL` | Regular goal |
| `PENALTY` | Penalty kick |
| `YELLOW_CARD` | Yellow card |
| `RED_CARD` | Red card |
| `OWN_GOAL` | Own goal |
| `SUBSTITUTION` | Player substitution |

---

## 6. Seed Data Summary

All seed data from `V2__seed_initial_data.sql`.

### Leagues

| ID (truncated) | Name | Slug | Sport | Country |
|---|---|---|---|---|
| `1111…1111` | SuperSport HNL | supersport-hnl | FOOTBALL | Croatia |

### Teams

| ID (suffix) | Name | Code | Stadium | City | Founded |
|---|---|---|---|---|---|
| `…2201` | GNK Dinamo Zagreb | DIN | Stadion Maksimir | Zagreb | 1911 |
| `…2202` | HNK Hajduk Split | HAJ | Stadion Poljud | Split | 1911 |
| `…2203` | HNK Rijeka | RIJ | Stadion Rujevica | Rijeka | 1906 |
| `…2204` | NK Osijek | OSI | Opus Arena | Osijek | 1947 |

### Seasons

| ID (truncated) | League | Name | Years | Status |
|---|---|---|---|---|
| `3333…3333` | SuperSport HNL | 2026/2027 Season | 2026–2027 | ACTIVE |

### Season Teams (Enrollments)

| ID (suffix) | Season | Team | Seed | Status |
|---|---|---|---|---|
| `…4401` | 2026/2027 Season | GNK Dinamo Zagreb | 1 | CONFIRMED |
| `…4402` | 2026/2027 Season | HNK Hajduk Split | 2 | CONFIRMED |
| `…4403` | 2026/2027 Season | HNK Rijeka | 3 | CONFIRMED |
| `…4404` | 2026/2027 Season | NK Osijek | 4 | CONFIRMED |

### Rounds

| ID (suffix) | Season | # | Name | Dates | Status |
|---|---|---|---|---|---|
| `…5501` | 2026/2027 Season | 1 | Round 1 | 2026-08-01 → 2026-08-03 | COMPLETED |

### Fixtures

| ID (suffix) | Round | Home | Away | Score | Status | Scheduled |
|---|---|---|---|---|---|---|
| `…6601` | Round 1 | Dinamo (DIN) | Hajduk (HAJ) | 2–1 | FINISHED | 2026-08-02 18:00 UTC |
| `…6602` | Round 1 | Rijeka (RIJ) | Osijek (OSI) | 1–1 | FINISHED | 2026-08-03 20:00 UTC |

### Match Events (Dinamo vs Hajduk only)

| ID (suffix) | Fixture | Team | Type | Min | Player | Assist | Notes |
|---|---|---|---|---|---|---|---|
| `…7701` | DIN vs HAJ | Dinamo | GOAL | 23′ | Bruno Petkovic | Martin Baturina | Left footed strike from penalty area |
| `…7702` | DIN vs HAJ | Hajduk | GOAL | 55′ | Marko Livaja | — | Header from corner kick |
| `…7703` | DIN vs HAJ | Dinamo | GOAL | 89′ | Martin Baturina | Josip Misic | Curling shot into top right corner |

---

## 7. Infrastructure & Configuration

### 7.1 Docker Compose — PostgreSQL

```mermaid
flowchart LR
    DC["docker-compose.yml"] --> PG["postgres:17-alpine\ncontainer: sports_league_postgres"]
    PG -- "port 5432:5432" --> APP["Spring Boot App\n(dev profile)"]
    PG --- VOL["Volume:\npostgres_data\n(local driver)"]

    style PG fill:#336791,color:#fff,stroke:#264f73
```

| Setting | Value |
|---|---|
| Image | `postgres:17-alpine` |
| Container name | `sports_league_postgres` |
| Port mapping | `5432:5432` |
| `POSTGRES_DB` | `sports_league_db` |
| `POSTGRES_USER` | `league_admin` |
| Restart policy | `unless-stopped` |
| Healthcheck | `pg_isready -U league_admin -d sports_league_db` (10s interval, 5s timeout, 5 retries) |
| Volume | `postgres_data` (local driver, persistent) |

### 7.2 Application Profiles

```mermaid
flowchart TB
    subgraph Profiles["spring.profiles"]
        DEV["dev (default active)"]
        PROD["prod"]
        TEST["test (application.yml\nin src/test/resources)"]
    end

    DEV -- "jdbc:postgresql://\nlocalhost:5432/\nsports_league_db" --> PG_DEV["PostgreSQL 17\n(Docker local)"]
    PROD -- "env vars:\nSPRING_DATASOURCE_URL\nSPRING_DATASOURCE_USERNAME\nSPRING_DATASOURCE_PASSWORD" --> PG_PROD["PostgreSQL 17\n(production)"]
    TEST -- "jdbc:h2:mem:\nsports_league_testdb\nMODE=PostgreSQL" --> H2["H2 In-Memory"]

    style DEV fill:#59a14f,color:#fff
    style PROD fill:#e15759,color:#fff
    style TEST fill:#4e79a7,color:#fff
```

| Property | dev | prod | test |
|---|---|---|---|
| Datasource URL | `jdbc:postgresql://localhost:5432/sports_league_db` | `${SPRING_DATASOURCE_URL}` | `jdbc:h2:mem:sports_league_testdb;…MODE=PostgreSQL` |
| Driver | `org.postgresql.Driver` | `org.postgresql.Driver` | `org.h2.Driver` |
| Dialect | *(auto-detected)* | *(auto-detected)* | `org.hibernate.dialect.H2Dialect` |
| `ddl-auto` | `validate` | `validate` | `validate` |
| `open-in-view` | `false` | `false` | `false` |
| `show-sql` | `false` | `false` | *(not set)* |
| Flyway enabled | `true` | `true` | `true` |
| Flyway locations | `classpath:db/migration` | `classpath:db/migration` | `classpath:db/migration` |
| Flyway baseline-on-migrate | `true` | `true` | *(not set)* |

### 7.3 HikariCP Connection Pool

| Setting | dev | prod |
|---|---|---|
| Pool name | `SportsLeagueHikariCP` | `SportsLeagueHikariCP-Prod` |
| Maximum pool size | 10 | 5 |
| Minimum idle | 5 | 2 |
| Idle timeout | 300 000 ms (5 min) | 300 000 ms (5 min) |
| Connection timeout | 20 000 ms (20 s) | 20 000 ms (20 s) |
| Max lifetime | 1 200 000 ms (20 min) | 1 200 000 ms (20 min) |
| Leak detection threshold | 30 000 ms (30 s) | *(not set)* |

### 7.4 Hibernate Batch Settings (dev profile)

| Property | Value |
|---|---|
| `hibernate.jdbc.batch_size` | 25 |
| `hibernate.order_inserts` | true |
| `hibernate.order_updates` | true |
| `hibernate.format_sql` | true |

### 7.5 Flyway Migration Chain

```mermaid
flowchart LR
    B["Baseline"] --> V1["V1__init_schema.sql\n8 tables, 10 indexes,\n2 CHECK constraints,\n3 UNIQUE constraints"]
    V1 --> V2["V2__seed_initial_data.sql\n1 league, 4 teams,\n1 season, 4 enrollments,\n1 round, 2 fixtures,\n3 match events"]

    style V1 fill:#4e79a7,color:#fff
    style V2 fill:#59a14f,color:#fff
```
