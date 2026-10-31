package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLeagueRequest(
        @NotBlank(message = "League name must not be blank")
        String name,

        @NotBlank(message = "Country must not be blank")
        String country,

        String sportType
) {}
