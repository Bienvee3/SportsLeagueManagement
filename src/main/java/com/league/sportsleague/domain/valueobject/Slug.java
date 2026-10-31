package com.league.sportsleague.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record Slug(String value) implements Comparable<Slug> {

    private static final Pattern VALID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public Slug {
        Objects.requireNonNull(value, "Slug must not be null");
        if (value.isBlank() || value.length() > 100 || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Invalid slug: '" + value + "'. Must match [a-z0-9]+(-[a-z0-9]+)* and be ≤100 chars");
        }
    }

    @Override
    public int compareTo(Slug other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
