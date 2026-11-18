# Performance: the N+1 problem in the season calendar

## 1. The problem

`Fixture.homeTeam`, `Fixture.awayTeam` and `Fixture.round` are `@ManyToOne(fetch = FetchType.LAZY)`. This is a
deliberate choice (see ADR 001): a match is loaded in many places that never look at its teams, and `EAGER`
would join or select them every time.

The price is that code which *does* read them pays one extra `SELECT` per distinct team and round the first
time it touches each proxy. The season calendar reads, for every match, both team names and the round, so a
plain "list the matches of a season" query triggers the classic N+1 pattern: 1 query for the list + N lazy
loads.

## 2. How it was found and measured

`NPlusOneMeasurementTest` (`infrastructure/persistence`) builds a season with 12 teams, 6 rounds and 36 matches,
then runs the calendar's access pattern (team names and round name of every match) over two variants. It counts
the SQL statements Hibernate really prepares with `hibernate.generate_statistics`
(`Statistics.getPrepareStatementCount()`) and logs the SQL with `org.hibernate.SQL=DEBUG`.

* **Before** — a plain JPQL query, no fetching:
  `SELECT f FROM Fixture f WHERE f.round.season.id = :sid ORDER BY f.scheduledTime`
* **After** — `FixtureRepository.findAllBySeasonWithTeams(seasonId)`, which uses `JOIN FETCH`:
  `SELECT f FROM Fixture f JOIN FETCH f.homeTeam JOIN FETCH f.awayTeam JOIN FETCH f.round WHERE f.round.season.id = :sid ORDER BY f.round.roundNumber, f.scheduledTime`

## 3. SQL before

One query for the list:

```sql
select f1_0.id, f1_0.away_score, f1_0.away_team_id, f1_0.home_score, f1_0.home_team_id,
       f1_0.postponement_reason, f1_0.round_id, f1_0.scheduled_time, f1_0.status
from fixtures f1_0
join rounds r1_0 on r1_0.id = f1_0.round_id
where r1_0.season_id = ?
order by f1_0.scheduled_time
```

then, while the page walks the matches, one lazy load per distinct team (12 times) and per distinct round
(6 times):

```sql
select t1_0.id, t1_0.city, t1_0.created_at, t1_0.founded_year, t1_0.name,
       t1_0.short_code, t1_0.sport_type, t1_0.stadium_name
from teams t1_0 where t1_0.id = ?          -- x 12

select r1_0.id, r1_0.end_date, r1_0.name, r1_0.round_number, r1_0.season_id, r1_0.start_date, r1_0.status
from rounds r1_0 where r1_0.id = ?         -- x 6
```

## 4. SQL after

A single statement that brings the match, both teams and the round:

```sql
select f1_0.id, f1_0.away_score, f1_0.away_team_id, at1_0.id, at1_0.city, at1_0.created_at, at1_0.founded_year,
       at1_0.name, at1_0.short_code, at1_0.sport_type, at1_0.stadium_name, f1_0.home_score, f1_0.home_team_id,
       ht1_0.id, ht1_0.city, ht1_0.created_at, ht1_0.founded_year, ht1_0.name, ht1_0.short_code,
       ht1_0.sport_type, ht1_0.stadium_name, f1_0.postponement_reason, f1_0.round_id,
       r1_0.id, r1_0.end_date, r1_0.name, r1_0.round_number, r1_0.season_id, r1_0.start_date, r1_0.status,
       f1_0.scheduled_time, f1_0.status
from fixtures f1_0
join teams ht1_0 on ht1_0.id = f1_0.home_team_id
join teams at1_0 on at1_0.id = f1_0.away_team_id
join rounds r1_0 on r1_0.id = f1_0.round_id
where r1_0.season_id = ?
order by r1_0.round_number, f1_0.scheduled_time
```

## 5. Numbers

Measured on 2026-11-19 with the test above (H2 in PostgreSQL mode, in memory, 36 matches / 12 teams / 6 rounds;
time is the median of 25 runs after 5 warm-up runs):

| | SQL statements | Median time |
|---|---:|---:|
| Before (lazy loading) | **19** (1 + 12 teams + 6 rounds) | 18 ms |
| After (`JOIN FETCH`) | **1** | 6 ms |
| Improvement | 19x fewer statements | about 3x faster |

The statement count is exact and does not depend on the machine. The time is indicative: on H2 in memory a
round trip costs almost nothing. With a network database (PostgreSQL on Neon) every extra statement is a network
round trip, so the gap grows with the latency; that effect was not measured here.

The count grows with the number of **distinct** teams and rounds a season has (the persistence context
de-duplicates repeated proxies), so it is `1 + teams + rounds`, not `1 + matches`. The fixed version stays at 1.

## 6. Where the fix is used and how it is protected

* `FixtureRepository.findAllBySeasonWithTeams` (calendar, dashboard) and `findFinishedBySeasonWithTeams`
  (standings) both use `JOIN FETCH`; `findScheduledBefore` (overdue-match job) and
  `RoundRepository.findOpenRoundsOfActiveSeasons` (round lifecycle job) do too.
* The match collections that are walked for many matches (`Fixture.events`, `Fixture.lineupEntries`) use
  `@BatchSize(50)`: they load for up to 50 matches per statement instead of one each, without the row
  multiplication a second `JOIN FETCH` of a collection would cause next to the teams.
* `NPlusOneMeasurementTest` fails if the calendar query ever takes more than one statement, or if the lazy
  baseline stops being `1 + teams + rounds`. `RepositorySliceTest` additionally checks that the fetched
  associations are initialised outside a transaction.
