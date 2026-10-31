package com.league.sportsleague.domain.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SlugTest {

    // --- Valid slugs ---

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abc-def", "a-b-c", "123", "a1b2c3", "supersport-hnl", "a"})
    @DisplayName("Should accept valid slug patterns")
    void shouldAcceptValidSlugs(String value) {
        Slug slug = new Slug(value);
        assertEquals(value, slug.value());
    }

    @Test
    @DisplayName("Should accept slug of exactly 100 characters")
    void shouldAcceptSlugOfMaxLength() {
        String value = "a".repeat(50) + "-" + "b".repeat(49); // 101 chars with dash = 101? No: 50+1+49=100
        Slug slug = new Slug(value);
        assertEquals(100, slug.value().length());
    }

    // --- Invalid slugs ---

    @Test
    @DisplayName("Should reject null slug")
    void shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> new Slug(null));
    }

    @Test
    @DisplayName("Should reject blank slug")
    void shouldRejectBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Slug(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "Abc", "aBc"})
    @DisplayName("Should reject uppercase letters")
    void shouldRejectUppercase(String value) {
        assertThrows(IllegalArgumentException.class, () -> new Slug(value));
    }

    @Test
    @DisplayName("Should reject slug with spaces")
    void shouldRejectSpaces() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("abc def"));
    }

    @Test
    @DisplayName("Should reject slug with double dash")
    void shouldRejectDoubleDash() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("abc--def"));
    }

    @Test
    @DisplayName("Should reject slug with leading dash")
    void shouldRejectLeadingDash() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("-abc"));
    }

    @Test
    @DisplayName("Should reject slug with trailing dash")
    void shouldRejectTrailingDash() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("abc-"));
    }

    @Test
    @DisplayName("Should reject slug exceeding 100 characters")
    void shouldRejectSlugOver100Chars() {
        String value = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> new Slug(value));
    }

    @Test
    @DisplayName("Should reject slug with special characters")
    void shouldRejectSpecialChars() {
        assertThrows(IllegalArgumentException.class, () -> new Slug("abc_def"));
    }

    // --- Value semantics ---

    @Test
    @DisplayName("Should have value-based equality")
    void shouldHaveValueEquality() {
        Slug slug1 = new Slug("supersport-hnl");
        Slug slug2 = new Slug("supersport-hnl");
        assertEquals(slug1, slug2);
        assertEquals(slug1.hashCode(), slug2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
        assertNotEquals(new Slug("abc"), new Slug("def"));
    }

    @Test
    @DisplayName("toString should return the raw value")
    void toStringShouldReturnRawValue() {
        assertEquals("supersport-hnl", new Slug("supersport-hnl").toString());
    }

    @Test
    @DisplayName("compareTo should order alphabetically")
    void shouldOrderAlphabetically() {
        assertTrue(new Slug("abc").compareTo(new Slug("def")) < 0);
        assertTrue(new Slug("def").compareTo(new Slug("abc")) > 0);
        assertEquals(0, new Slug("abc").compareTo(new Slug("abc")));
    }
}
