package com.league.sportsleague.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LineupRules")
class LineupRulesTest {

    @Test
    @DisplayName("FOOTBALL rules: 11 starters, up to 12 bench, 5 substitutions, no re-entry, 90 minutes")
    void footballRules() {
        LineupRules r = LineupRules.FOOTBALL;
        assertEquals(11, r.starters());
        assertEquals(12, r.maxBench());
        assertEquals(5, r.maxSubstitutions());
        assertFalse(r.reentryAllowed());
        assertEquals(90, r.regulationMinutes());
    }

    @Test
    @DisplayName("BASKETBALL rules: 5 starters, up to 7 bench, unlimited substitutions with re-entry, 40 minutes")
    void basketballRules() {
        LineupRules r = LineupRules.BASKETBALL;
        assertEquals(5, r.starters());
        assertEquals(7, r.maxBench());
        assertEquals(Integer.MAX_VALUE, r.maxSubstitutions());
        assertTrue(r.reentryAllowed());
        assertEquals(40, r.regulationMinutes());
    }

    @Test
    @DisplayName("VOLLEYBALL rules: no game clock, regulation minutes are 0")
    void volleyballRules() {
        assertEquals(0, LineupRules.VOLLEYBALL.regulationMinutes());
    }

    @ParameterizedTest
    @CsvSource({
            "FOOTBALL, FOOTBALL",
            "football, FOOTBALL",
            "  Football  , FOOTBALL",
            "BASKETBALL, BASKETBALL",
            "handball, HANDBALL",
            "Volleyball, VOLLEYBALL"
    })
    @DisplayName("forSport maps case-insensitively with trimming")
    void forSportMapsKnownSports(String input, LineupRules expected) {
        assertEquals(expected, LineupRules.forSport(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "CRICKET", "RUGBY", "tennis"})
    @DisplayName("forSport falls back to FOOTBALL for null, blank or unknown sports")
    void forSportFallbackToFootball(String input) {
        assertEquals(LineupRules.FOOTBALL, LineupRules.forSport(input));
    }
}
