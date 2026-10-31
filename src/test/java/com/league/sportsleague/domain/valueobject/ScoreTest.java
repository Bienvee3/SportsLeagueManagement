package com.league.sportsleague.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreTest {

    // --- Valid scores ---

    @Test
    @DisplayName("Should accept 0-0 score")
    void shouldAcceptZeroZero() {
        Score score = new Score(0, 0);
        assertEquals(0, score.home());
        assertEquals(0, score.away());
    }

    @Test
    @DisplayName("Should accept typical score 3-1")
    void shouldAcceptTypicalScore() {
        Score score = new Score(3, 1);
        assertEquals(3, score.home());
        assertEquals(1, score.away());
    }

    @Test
    @DisplayName("Should accept large score 99-0")
    void shouldAcceptLargeScore() {
        Score score = new Score(99, 0);
        assertEquals(99, score.home());
    }

    // --- Invalid scores ---

    @Test
    @DisplayName("Should reject negative home score")
    void shouldRejectNegativeHome() {
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, 0));
    }

    @Test
    @DisplayName("Should reject negative away score")
    void shouldRejectNegativeAway() {
        assertThrows(IllegalArgumentException.class, () -> new Score(0, -1));
    }

    @Test
    @DisplayName("Should reject both negative scores")
    void shouldRejectBothNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Score(-1, -1));
    }

    // --- Value semantics ---

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueEquality() {
        assertEquals(new Score(2, 1), new Score(2, 1));
        assertEquals(new Score(2, 1).hashCode(), new Score(2, 1).hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
        assertNotEquals(new Score(2, 1), new Score(1, 2));
    }

    @Test
    @DisplayName("toString should return home-away format")
    void toStringShouldReturnHomeAwayFormat() {
        assertEquals("3-1", new Score(3, 1).toString());
        assertEquals("0-0", new Score(0, 0).toString());
    }
}
