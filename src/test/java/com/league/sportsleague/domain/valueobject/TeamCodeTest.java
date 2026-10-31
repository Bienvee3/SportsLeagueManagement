package com.league.sportsleague.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TeamCodeTest {

    // --- Valid codes ---

    @ParameterizedTest
    @ValueSource(strings = {"AB", "DIN", "HAJ", "RIJ", "OSI", "ABCDEFGHIJ"})
    @DisplayName("Should accept valid uppercase alpha codes of 2-10 characters")
    void shouldAcceptValidCodes(String value) {
        TeamCode code = new TeamCode(value);
        assertEquals(value, code.value());
    }

    // --- Invalid codes ---

    @Test
    @DisplayName("Should reject null code")
    void shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> new TeamCode(null));
    }

    @Test
    @DisplayName("Should reject empty code")
    void shouldRejectEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode(""));
    }

    @Test
    @DisplayName("Should reject single character (too short)")
    void shouldRejectTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode("A"));
    }

    @Test
    @DisplayName("Should reject 11-character code (too long)")
    void shouldRejectTooLong() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode("ABCDEFGHIJK"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"din", "Din", "dIN"})
    @DisplayName("Should reject lowercase letters")
    void shouldRejectLowercase(String value) {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode(value));
    }

    @Test
    @DisplayName("Should reject code with digits")
    void shouldRejectDigits() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode("DI1"));
    }

    @Test
    @DisplayName("Should reject code with hyphen")
    void shouldRejectHyphen() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode("DI-N"));
    }

    @Test
    @DisplayName("Should reject code with space")
    void shouldRejectSpace() {
        assertThrows(IllegalArgumentException.class, () -> new TeamCode("DI N"));
    }

    // --- Value semantics ---

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueEquality() {
        TeamCode code1 = new TeamCode("DIN");
        TeamCode code2 = new TeamCode("DIN");
        assertEquals(code1, code2);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
        assertNotEquals(new TeamCode("DIN"), new TeamCode("HAJ"));
    }

    @Test
    @DisplayName("toString should return the raw value")
    void toStringShouldReturnRawValue() {
        assertEquals("DIN", new TeamCode("DIN").toString());
    }
}
