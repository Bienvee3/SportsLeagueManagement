package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLeagueRequest(
        @NotBlank(message = "{validation.league.name.required}")
        String name,

        @NotBlank(message = "{validation.league.slug.required}")
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                 message = "{validation.league.slug.pattern}")
        String slug,

        String sportType,

        @NotBlank(message = "{validation.league.country.required}")
        String country
) {}
