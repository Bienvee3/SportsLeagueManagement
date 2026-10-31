package com.league.sportsleague.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record TeamCode(String value) {

    private static final Pattern VALID = Pattern.compile("[A-Z]{2,10}");

    public TeamCode {
        Objects.requireNonNull(value, "TeamCode must not be null");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid team code: '" + value + "'. Must be 2–10 uppercase letters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
