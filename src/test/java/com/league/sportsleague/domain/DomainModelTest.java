package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {

    @Test
    @DisplayName("Should successfully construct aggregate hierarchy with bidirectional integrity")
    void shouldConstructAggregateHierarchy() {
        // 1. League
        League league = new League("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
        assertEquals("supersport-hnl", league.getSlug().value());

        // 2. Teams
        Team dinamo = new Team("GNK Dinamo Zagreb", "DIN", "Maksimir", "Zagreb", 1911);
        Team hajduk = new Team("HNK Hajduk Split", "HAJ", "Poljud", "Split", 1911);

        // 3. Season
        Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
        league.addSeason(season);
        assertEquals(1, league.getSeasons().size());
        assertEquals(league, season.getLeague());

        // 4. Enroll Teams (SeasonTeam association entity)
        SeasonTeam stDinamo = new SeasonTeam(season, dinamo, 1, SeasonTeamStatus.CONFIRMED);
        SeasonTeam stHajduk = new SeasonTeam(season, hajduk, 2, SeasonTeamStatus.CONFIRMED);
        season.addSeasonTeam(stDinamo);
        season.addSeasonTeam(stHajduk);
        assertEquals(2, season.getSeasonTeams().size());

        // 5. Round
        Round round1 = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(2), RoundStatus.SCHEDULED);
        season.addRound(round1);
        assertEquals(1, season.getRounds().size());
        assertEquals(season, round1.getSeason());

        // 6. Fixture
        Fixture derby = new Fixture(round1, dinamo, hajduk, Instant.now());
        round1.addFixture(derby);
        assertEquals(1, round1.getFixtures().size());
        assertEquals(round1, derby.getRound());

        // 7. Match events (goals only before the final whistle) & result
        MatchEvent goal1 = new MatchEvent(derby, dinamo, MatchEventType.GOAL, 23, "Bruno Petkovic");
        MatchEvent goal2 = new MatchEvent(derby, hajduk, MatchEventType.GOAL, 55, "Marko Livaja");
        MatchEvent goal3 = new MatchEvent(derby, dinamo, MatchEventType.GOAL, 89, "Martin Baturina");
        derby.addMatchEvent(goal1);
        derby.addMatchEvent(goal2);
        derby.addMatchEvent(goal3);
        assertEquals(3, derby.getEvents().size());
        assertEquals(derby, goal1.getFixture());
        assertEquals(2, derby.getHomeScore());
        assertEquals(1, derby.getAwayScore());

        derby.recordResult(new Score(2, 1));
        assertEquals(FixtureStatus.FINISHED, derby.getStatus());
        assertEquals(2, derby.getHomeScore());
        assertEquals(1, derby.getAwayScore());

        // 8. Result correction with audit — uses domain method correctResult()
        derby.correctResult(new Score(2, 2), "VAR review validated goal after final whistle", "Referee Official");
        assertEquals(1, derby.getAudits().size());
        assertEquals("Referee Official", derby.getAudits().get(0).getCorrectedBy());
        assertEquals(2, derby.getAudits().get(0).getOldHomeScore());
        assertEquals(1, derby.getAudits().get(0).getOldAwayScore());
        assertEquals(2, derby.getHomeScore());
        assertEquals(2, derby.getAwayScore());
    }

    @Test
    @DisplayName("Should maintain business key equality semantics")
    void shouldMaintainBusinessKeyEquality() {
        League league1 = new League("League A", "league-slug", "FOOTBALL", "Croatia");
        League league2 = new League("League B (renamed)", "league-slug", "FOOTBALL", "Croatia");
        assertEquals(league1, league2);
        assertEquals(league1.hashCode(), league2.hashCode());

        Team team1 = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
        Team team2 = new Team("Dinamo New Name", "DIN", "New Stadium", "Zagreb", 1911);
        assertEquals(team1, team2);
        assertEquals(team1.hashCode(), team2.hashCode());
    }

    @Test
    @DisplayName("Should protect collections against direct external mutation")
    void shouldProtectCollectionsAgainstDirectExternalMutation() {
        League league = new League("LaLiga", "laliga", "FOOTBALL", "Spain");
        Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);

        // Attempting to modify unmodifiable collection directly must fail
        assertThrows(UnsupportedOperationException.class, () -> league.getSeasons().add(season));

        // Adding via domain helper method must succeed
        league.addSeason(season);
        assertEquals(1, league.getSeasons().size());
    }
}
