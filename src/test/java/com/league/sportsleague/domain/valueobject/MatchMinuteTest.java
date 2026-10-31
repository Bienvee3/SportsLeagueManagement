package com.league.sportsleague.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MatchMinuteTest {

    // --- Valid minutes ---

    @ParameterizedTest
    @ValueSource(ints = {1, 23, 45, 90, 120, 130})
    @DisplayName("Should accept valid match minutes between 1 and 130")
    void shouldAcceptValidMinutes(int value) {
        MatchMinute minute = new MatchMinute(value);
        assertEquals(value, minute.value());
    }

    // --- Invalid minutes ---

    @Test
    @DisplayName("Should reject minute of 0")
    void shouldRejectZero() {
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(0));
    }

    @Test
    @DisplayName("Should reject negative minute")
    void shouldRejectNegative() {
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(-1));
    }

    @Test
    @DisplayName("Should reject minute of 131")
    void shouldReject131() {
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(131));
    }

    @Test
    @DisplayName("Should reject very large minute")
    void shouldRejectVeryLarge() {
        assertThrows(IllegalArgumentException.class, () -> new MatchMinute(999));
    }

    // --- Value semantics ---

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueEquality() {
        MatchMinute m1 = new MatchMinute(23);
        MatchMinute m2 = new MatchMinute(23);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
        assertNotEquals(new MatchMinute(23), new MatchMinute(45));
    }

    @Test
    @DisplayName("toString should return the minute as string")
    void toStringShouldReturnMinuteAsString() {
        assertEquals("23", new MatchMinute(23).toString());
    }
}
