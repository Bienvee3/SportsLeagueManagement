package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.OverdueFixtureResponse;
import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import com.league.sportsleague.domain.repository.FixtureRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OverdueFixtureServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @Mock FixtureRepository fixtureRepository;
    @InjectMocks OverdueFixtureService service;

    @Test
    @DisplayName("asks for SCHEDULED matches that should have started before now minus the grace period")
    void usesCutoffOfNowMinusGrace() {
        when(fixtureRepository.findScheduledBefore(NOW.minus(Duration.ofHours(3)))).thenReturn(List.of());

        assertTrue(service.findOverdue(NOW, Duration.ofHours(3)).isEmpty());

        verify(fixtureRepository).findScheduledBefore(Instant.parse("2026-11-14T09:00:00Z"));
    }

    @Test
    @DisplayName("maps each overdue match to a response with teams, round and kick-off time")
    void mapsToResponse() {
        League league = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
        Round round = new Round(season, 4, "Round 4", null, null, RoundStatus.SCHEDULED);
        Team home = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
        Team away = new Team("Hajduk", "HAJ", "Poljud", "Split", 1911);
        Instant kickOff = Instant.parse("2026-11-14T08:00:00Z");
        Fixture late = new Fixture(round, home, away, kickOff);
        when(fixtureRepository.findScheduledBefore(NOW.minus(Duration.ofHours(2)))).thenReturn(List.of(late));

        List<OverdueFixtureResponse> result = service.findOverdue(NOW, Duration.ofHours(2));

        assertEquals(1, result.size());
        OverdueFixtureResponse r = result.get(0);
        assertEquals(late.getId(), r.fixtureId());
        assertEquals("Dinamo", r.homeTeam());
        assertEquals("Hajduk", r.awayTeam());
        assertEquals(4, r.roundNumber());
        assertEquals(kickOff, r.scheduledTime());
    }
}
