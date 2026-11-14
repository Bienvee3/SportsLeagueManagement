package com.league.sportsleague.web.view;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats dates and numbers for the language of the current request. Used from templates as
 * {@code ${@fmt.dateTime(x)}}, {@code ${@fmt.date(x)}} and {@code ${@fmt.number(x)}}. The patterns live in
 * the {@code common.format.*} message keys (one per language); instants are shown in UTC, the zone match
 * times are entered in.
 */
@Component("fmt")
public class DisplayFormats {

    private final MessageSource messageSource;

    public DisplayFormats(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String dateTime(Instant instant) {
        if (instant == null) {
            return "";
        }
        return formatter("common.format.dateTime").withZone(ZoneOffset.UTC).format(instant);
    }

    public String date(LocalDate date) {
        return date == null ? "" : formatter("common.format.date").format(date);
    }

    public String number(Number number) {
        return number == null ? "" : NumberFormat.getNumberInstance(LocaleContextHolder.getLocale()).format(number);
    }

    private DateTimeFormatter formatter(String patternKey) {
        Locale locale = LocaleContextHolder.getLocale();
        return DateTimeFormatter.ofPattern(messageSource.getMessage(patternKey, null, locale), locale);
    }
}
