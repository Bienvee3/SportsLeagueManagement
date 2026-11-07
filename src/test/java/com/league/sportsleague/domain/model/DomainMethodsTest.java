package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainMethodsTest {

    private League league() {
        return new League("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
    }

    private Team team(String code) {
        return new Team("Team " + code, code, "Stadium", "City", 2000);
    }

    private Season activeSeason() {
        Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
        s.activate();
        return s;
    }

    private Round scheduledRound(Season season) {
        return new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(3), RoundStatus.SCHEDULED);
    }

    private Fixture finishedFixture(Round round, Score score) {
        Fixture f = new Fixture(round, team("HOM"), team("AWY"), Instant.now());
        f.recordResult(score);
        return f;
    }

    // ── League.rename() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("League.rename()")
    class LeagueRename {

        @Test
        @DisplayName("rename() updates name, country, and sportType")
        void renameUpdatesFields() {
            League l = league();
            l.rename("New Name", "Spain", "BASKETBALL");
            assertEquals("New Name", l.getName());
            assertEquals("Spain", l.getCountry());
            assertEquals("BASKETBALL", l.getSportType());
        }

        @Test
        @DisplayName("rename() with null sportType keeps existing value")
        void renameWithNullSportTypeKeepsExisting() {
            League l = league();
            l.rename("New Name", "Spain", null);
            assertEquals("FOOTBALL", l.getSportType());
        }

        @Test
        @DisplayName("rename() rejects blank name")
        void renameRejectsBlankName() {
            League l = league();
            assertThrows(IllegalArgumentException.class, () -> l.rename("  ", "Spain", null));
        }

        @Test
        @DisplayName("rename() rejects null name")
        void renameRejectsNullName() {
            League l = league();
            assertThrows(IllegalArgumentException.class, () -> l.rename(null, "Spain", null));
        }
    }

    // ── Team.updateProfile() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Team.updateProfile()")
    class TeamUpdateProfile {

        @Test
        @DisplayName("updateProfile() updates team details")
        void updateProfileUpdatesFields() {
            Team t = team("DIN");
            t.updateProfile("New Name", "New Stadium", "New City", 2005);
            assertEquals("New Name", t.getName());
            assertEquals("New Stadium", t.getStadiumName());
            assertEquals("New City", t.getCity());
            assertEquals(2005, t.getFoundedYear());
        }

        @Test
        @DisplayName("updateProfile() rejects blank name")
        void updateProfileRejectsBlankName() {
            Team t = team("DIN");
            assertThrows(IllegalArgumentException.class,
                () -> t.updateProfile("  ", "Stadium", "City", 2000));
        }
    }

    @Nested
    @DisplayName("Team.sportType")
    class TeamSportType {

        @Test
        @DisplayName("defaults to FOOTBALL when not specified")
        void defaultsToFootball() {
            assertEquals("FOOTBALL", team("DIN").getSportType());
        }

        @Test
        @DisplayName("is normalised to upper case and trimmed")
        void normalisesSport() {
            Team t = new Team("Cibona", "CIB", " basketball ", "Hall", "Zagreb", 1946);
            assertEquals("BASKETBALL", t.getSportType());
        }

        @Test
        @DisplayName("blank sport falls back to FOOTBALL")
        void blankSportFallsBack() {
            assertEquals("FOOTBALL", new Team("X", "XXX", "  ", "S", "C", 2000).getSportType());
        }
    }

    @Nested
    @DisplayName("Round.renumber()")
    class RoundRenumber {

        @Test
        @DisplayName("updates the number and the default 'Round N' name")
        void renumbersDefaultName() {
            Season s = activeSeason();
            Round r = new Round(s, 3, "Round 3", null, null, null);
            r.renumber(2);
            assertEquals(2, r.getRoundNumber());
            assertEquals("Round 2", r.getName());
        }

        @Test
        @DisplayName("keeps a custom name")
        void keepsCustomName() {
            Season s = activeSeason();
            Round r = new Round(s, 3, "Derby week", null, null, null);
            r.renumber(2);
            assertEquals(2, r.getRoundNumber());
            assertEquals("Derby week", r.getName());
        }

        @Test
        @DisplayName("rejects numbers below 1")
        void rejectsInvalidNumber() {
            Round r = new Round(activeSeason(), 1, "Round 1", null, null, null);
            assertThrows(IllegalArgumentException.class, () -> r.renumber(0));
        }
    }

    // ── Fixture.correctResult() ────────────────────────────────────────────────

    @Nested
    @DisplayName("Fixture.correctResult()")
    class FixtureCorrectResult {

        @Test
        @DisplayName("correctResult() creates audit, updates score, keeps FINISHED status")
        void correctResultCreatesAuditAndUpdatesScore() {
            Round r = scheduledRound(activeSeason());
            Fixture f = finishedFixture(r, new Score(2, 1));

            f.correctResult(new Score(2, 2), "VAR review", "Referee");

            assertEquals(2, f.getHomeScore());
            assertEquals(2, f.getAwayScore());
            assertEquals(FixtureStatus.FINISHED, f.getStatus());
            assertEquals(1, f.getAudits().size());

            ResultAudit audit = f.getAudits().get(0);
            assertEquals(2, audit.getOldHomeScore());
            assertEquals(1, audit.getOldAwayScore());
            assertEquals(2, audit.getNewHomeScore());
            assertEquals(2, audit.getNewAwayScore());
            assertEquals("VAR review", audit.getReason());
            assertEquals("Referee", audit.getCorrectedBy());
        }

        @Test
        @DisplayName("correctResult() accumulates multiple audits")
        void correctResultAccumulatesAudits() {
            Round r = scheduledRound(activeSeason());
            Fixture f = finishedFixture(r, new Score(1, 0));
            f.correctResult(new Score(2, 0), "First correction", "Ref1");
            f.correctResult(new Score(2, 1), "Second correction", "Ref2");
            assertEquals(2, f.getAudits().size());
            assertEquals(2, f.getHomeScore());
            assertEquals(1, f.getAwayScore());
        }

        @Test
        @DisplayName("correctResult() throws when fixture is not FINISHED")
        void correctResultThrowsIfNotFinished() {
            Round r = scheduledRound(activeSeason());
            Fixture f = new Fixture(r, team("HOM"), team("AWY"), Instant.now());
            assertThrows(InvalidStateTransitionException.class,
                () -> f.correctResult(new Score(1, 0), "Reason", "Ref"));
        }

        @Test
        @DisplayName("correctResult() rejects null score")
        void correctResultRejectsNullScore() {
            Round r = scheduledRound(activeSeason());
            Fixture f = finishedFixture(r, new Score(1, 0));
            assertThrows(NullPointerException.class,
                () -> f.correctResult(null, "Reason", "Ref"));
        }
    }

    // ── Domain query methods ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Domain query methods")
    class DomainQueries {

        @Test
        @DisplayName("Season.isActive() returns true only when ACTIVE")
        void seasonIsActive() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            assertFalse(s.isActive());
            s.activate();
            assertTrue(s.isActive());
            s.complete();
            assertFalse(s.isActive());
        }

        @Test
        @DisplayName("Season.teamCount() returns enrolled team count")
        void seasonTeamCount() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            assertEquals(0, s.teamCount());
            s.addSeasonTeam(new SeasonTeam(s, team("DIN"), 1, SeasonTeamStatus.CONFIRMED));
            assertEquals(1, s.teamCount());
        }

        @Test
        @DisplayName("Fixture.isFinished() returns true only when FINISHED")
        void fixtureIsFinished() {
            Round r = scheduledRound(activeSeason());
            Fixture f = new Fixture(r, team("HOM"), team("AWY"), Instant.now());
            assertFalse(f.isFinished());
            f.recordResult(new Score(1, 0));
            assertTrue(f.isFinished());
        }

        @Test
        @DisplayName("Fixture.isDraw() returns true only when finished and scores equal")
        void fixtureIsDraw() {
            Round r = scheduledRound(activeSeason());
            Fixture win = new Fixture(r, team("HOM"), team("AWY"), Instant.now());
            win.recordResult(new Score(2, 1));
            assertFalse(win.isDraw());

            Round r2 = scheduledRound(activeSeason());
            Fixture draw = new Fixture(r2, team("HOA"), team("AWB"), Instant.now());
            draw.recordResult(new Score(1, 1));
            assertTrue(draw.isDraw());
        }

        @Test
        @DisplayName("Round.allFixturesFinished() returns false when no fixtures")
        void roundAllFinishedEmptyReturnsFalse() {
            Round r = scheduledRound(activeSeason());
            assertFalse(r.allFixturesFinished());
        }

        @Test
        @DisplayName("Round.allFixturesFinished() returns true when all finished")
        void roundAllFixturesFinishedWhenAllDone() {
            Round r = scheduledRound(activeSeason());
            Fixture f1 = new Fixture(r, team("HOM"), team("AWY"), Instant.now());
            f1.recordResult(new Score(1, 0));
            r.addFixture(f1);
            assertTrue(r.allFixturesFinished());
        }

        @Test
        @DisplayName("Round.allFixturesFinished() returns false when some pending")
        void roundAllFixturesFinishedWhenSomePending() {
            Round r = scheduledRound(activeSeason());
            Fixture f1 = new Fixture(r, team("HOM"), team("AWY"), Instant.now());
            f1.recordResult(new Score(1, 0));
            r.addFixture(f1);
            Fixture f2 = new Fixture(r, team("AAA"), team("BBB"), Instant.now());
            r.addFixture(f2);
            assertFalse(r.allFixturesFinished());
        }
    }
}
