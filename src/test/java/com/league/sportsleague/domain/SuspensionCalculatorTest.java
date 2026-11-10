package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import com.league.sportsleague.domain.model.enums.SuspensionReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SuspensionCalculator")
class SuspensionCalculatorTest {

    private static final Instant DAY_1 = Instant.parse("2026-08-01T18:00:00Z");

    private Round round;
    private Team dinamo;
    private Team hajduk;
    private Team rijeka;
    private Player petkovic;
    private Player baturina;
    private Player livaja;
    private final List<Fixture> season = new ArrayList<>();

    @BeforeEach
    void setUp() {
        League league = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        Season s = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
        round = new Round(s, 1, "Round 1", LocalDate.now(), LocalDate.now(), RoundStatus.SCHEDULED);
        dinamo = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
        hajduk = new Team("Hajduk", "HAJ", "Poljud", "Split", 1911);
        rijeka = new Team("Rijeka", "RIJ", "Rujevica", "Rijeka", 1906);
        petkovic = dinamo.addPlayer("Bruno Petkovic", 9, "Forward");
        baturina = dinamo.addPlayer("Martin Baturina", 10, "Midfielder");
        livaja = hajduk.addPlayer("Marko Livaja", 10, "Forward");
    }

    private Instant day(int n) {
        return DAY_1.plus(n - 1L, ChronoUnit.DAYS);
    }

    /** A fixture of the season on day {@code n}, still SCHEDULED. */
    private Fixture scheduled(int n, Team home, Team away) {
        Fixture f = new Fixture(round, home, away, day(n));
        season.add(f);
        return f;
    }

    /** A FINISHED fixture on day {@code n} with the given cards ({@code type} per player, at increasing minutes). */
    private Fixture played(int n, Team home, Team away, Object... typeAndPlayer) {
        Fixture f = scheduled(n, home, away);
        f.kickOff();
        for (int i = 0; i < typeAndPlayer.length; i += 2) {
            MatchEventType type = (MatchEventType) typeAndPlayer[i];
            Player player = (Player) typeAndPlayer[i + 1];
            f.addMatchEvent(new MatchEvent(f, player.getTeam(), type, 10 + i, player, null, null));
        }
        f.finish();
        return f;
    }

    private Map<java.util.UUID, SuspensionReason> suspended(Fixture target, Team team) {
        return SuspensionCalculator.suspendedFor(target, team, season);
    }

    @Test
    @DisplayName("a red card in the previous finished match suspends the player for the next one")
    void redCardSuspendsForNextMatch() {
        played(1, dinamo, hajduk, MatchEventType.RED_CARD, petkovic);
        Fixture next = scheduled(8, rijeka, dinamo);

        assertEquals(Map.of(petkovic.getId(), SuspensionReason.RED_CARD), suspended(next, dinamo));
        assertTrue(suspended(next, rijeka).isEmpty());
    }

    @Test
    @DisplayName("the ban is served in one match: the player is free again afterwards")
    void banServedInOneMatch() {
        played(1, dinamo, hajduk, MatchEventType.RED_CARD, petkovic);
        played(8, rijeka, dinamo);
        Fixture third = scheduled(15, dinamo, hajduk);

        assertTrue(suspended(third, dinamo).isEmpty());
    }

    @Test
    @DisplayName("the 5th yellow card of the season suspends the player; the 4th does not")
    void fifthYellowSuspends() {
        for (int n = 1; n <= 4; n++) {
            played(n, dinamo, rijeka, MatchEventType.YELLOW_CARD, baturina);
        }
        Fixture afterFour = scheduled(5, dinamo, hajduk);
        assertTrue(suspended(afterFour, dinamo).isEmpty());

        season.remove(afterFour);
        played(5, dinamo, hajduk, MatchEventType.YELLOW_CARD, baturina);
        Fixture afterFive = scheduled(6, dinamo, rijeka);

        assertEquals(Map.of(baturina.getId(), SuspensionReason.YELLOW_CARDS), suspended(afterFive, dinamo));
    }

    @Test
    @DisplayName("a red card and the 5th yellow in the same match count as one RED_CARD ban")
    void redAndFifthYellowInSameMatch() {
        for (int n = 1; n <= 4; n++) {
            played(n, dinamo, rijeka, MatchEventType.YELLOW_CARD, baturina);
        }
        played(5, dinamo, hajduk, MatchEventType.YELLOW_CARD, baturina, MatchEventType.RED_CARD, baturina);
        Fixture next = scheduled(6, dinamo, rijeka);

        assertEquals(Map.of(baturina.getId(), SuspensionReason.RED_CARD), suspended(next, dinamo));
    }

    @Test
    @DisplayName("cards of other teams are ignored")
    void otherTeamsIgnored() {
        played(1, dinamo, hajduk, MatchEventType.RED_CARD, livaja);
        Fixture next = scheduled(8, dinamo, rijeka);

        assertTrue(suspended(next, dinamo).isEmpty());
        assertEquals(Map.of(livaja.getId(), SuspensionReason.RED_CARD), suspended(scheduled(9, hajduk, rijeka), hajduk));
    }

    @Test
    @DisplayName("fixtures scheduled later or not finished are not predecessors")
    void laterOrUnfinishedFixturesIgnored() {
        Fixture target = scheduled(5, dinamo, rijeka);
        played(9, dinamo, hajduk, MatchEventType.RED_CARD, petkovic);   // after the target
        Fixture postponed = scheduled(3, hajduk, dinamo);
        postponed.postpone("Rain");                                      // before, but never played

        assertTrue(suspended(target, dinamo).isEmpty());
    }

    @Test
    @DisplayName("events recorded with a free-text name only do not count")
    void textOnlyEventsIgnored() {
        Fixture first = scheduled(1, dinamo, hajduk);
        first.kickOff();
        first.addMatchEvent(new MatchEvent(first, dinamo, MatchEventType.RED_CARD, 30, "Bruno Petkovic"));
        first.finish();
        Fixture next = scheduled(8, dinamo, rijeka);

        assertTrue(suspended(next, dinamo).isEmpty());
    }

    @Test
    @DisplayName("a team that has not played yet has no suspended players")
    void noPreviousMatch() {
        Fixture first = scheduled(1, dinamo, hajduk);

        assertTrue(suspended(first, dinamo).isEmpty());
    }
}
