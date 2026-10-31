package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLeagueRequest(
        @NotBlank(message = "League name must not be blank")
        String name,

        @NotBlank(message = "Slug must not be blank")
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                 message = "Slug must match [a-z0-9]+(-[a-z0-9]+)*")
        String slug,

        String sportType,

        @NotBlank(message = "Country must not be blank")
        String country
) {}
