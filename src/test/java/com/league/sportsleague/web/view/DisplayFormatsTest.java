package com.league.sportsleague.web.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayFormatsTest {

    private static final Locale CROATIAN = Locale.forLanguageTag("hr");
    private static final Instant KICK_OFF = Instant.parse("2026-08-02T18:05:00Z");

    private final DisplayFormats formats;

    DisplayFormatsTest() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/common");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        this.formats = new DisplayFormats(messages);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("English date-time: day, short month name, 24-hour clock, in UTC")
    void englishDateTime() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        assertEquals("2 Aug 2026, 18:05", formats.dateTime(KICK_OFF));
    }

    @Test
    @DisplayName("Croatian date-time: dd.MM.yyyy. with the trailing dot")
    void croatianDateTime() {
        LocaleContextHolder.setLocale(CROATIAN);

        assertEquals("02.08.2026. 18:05", formats.dateTime(KICK_OFF));
    }

    @Test
    @DisplayName("dates follow the language too")
    void dates() {
        LocalDate day = LocalDate.of(2026, 8, 2);

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("2 Aug 2026", formats.date(day));
        LocaleContextHolder.setLocale(CROATIAN);
        assertEquals("02.08.2026.", formats.date(day));
    }

    @Test
    @DisplayName("numbers use the language's grouping separator")
    void numbers() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertEquals("1,234,567", formats.number(1234567));
        LocaleContextHolder.setLocale(CROATIAN);
        assertEquals("1.234.567", formats.number(1234567));
    }

    @Test
    @DisplayName("null values render as an empty string instead of failing the page")
    void nullsAreEmpty() {
        assertEquals("", formats.dateTime(null));
        assertEquals("", formats.date(null));
        assertEquals("", formats.number(null));
    }
}
