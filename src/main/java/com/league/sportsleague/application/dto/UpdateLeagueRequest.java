package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLeagueRequest(
        @NotBlank(message = "{validation.league.name.required}")
        String name,

        @NotBlank(message = "{validation.league.country.required}")
        String country,

        String sportType
) {}
