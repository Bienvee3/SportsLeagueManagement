package com.league.sportsleague.application.service;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import com.league.sportsleague.domain.repository.RoundRepository;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundLifecycleServiceTest {

    @Mock RoundRepository roundRepository;
    @InjectMocks RoundLifecycleService service;

    private Season season;

    @BeforeEach
    void setUp() {
        League league = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
    }

    private Round roundWithOneFixture(int number) {
        Round round = new Round(season, number, "Round " + number, null, null, RoundStatus.SCHEDULED);
        Team home = new Team("Home " + number, "H" + (char) ('A' + number), "Stadium", "City", 2000);
        Team away = new Team("Away " + number, "W" + (char) ('A' + number), "Stadium", "City", 2000);
        round.addFixture(new Fixture(round, home, away, Instant.now()));
        return round;
    }

    @Test
    @DisplayName("advances the open rounds, saves only the ones that changed and reports how many")
    void advancesAndSavesOnlyChangedRounds() {
        Round finished = roundWithOneFixture(1);
        finished.getFixtures().get(0).recordResult(new Score(1, 0));
        Round untouched = roundWithOneFixture(2);
        when(roundRepository.findOpenRoundsOfActiveSeasons()).thenReturn(List.of(finished, untouched));

        int changed = service.advanceRounds();

        assertEquals(1, changed);
        assertEquals(RoundStatus.COMPLETED, finished.getStatus());
        assertEquals(RoundStatus.SCHEDULED, untouched.getStatus());
        verify(roundRepository).save(finished);
        verify(roundRepository, never()).save(untouched);
    }

    @Test
    @DisplayName("does nothing when there are no open rounds")
    void nothingToDo() {
        when(roundRepository.findOpenRoundsOfActiveSeasons()).thenReturn(List.of());

        assertEquals(0, service.advanceRounds());

        verify(roundRepository, never()).save(any());
    }

    @Test
    @DisplayName("running it twice is harmless: the second run changes nothing")
    void idempotent() {
        Round round = roundWithOneFixture(1);
        round.getFixtures().get(0).recordResult(new Score(2, 2));
        when(roundRepository.findOpenRoundsOfActiveSeasons()).thenReturn(List.of(round));

        assertEquals(1, service.advanceRounds());
        assertEquals(0, service.advanceRounds());
    }
}
