package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
        @NotBlank(message = "Team name must not be blank")
        String name,

        @NotBlank(message = "Short code must not be blank")
        String shortCode,

        @NotBlank(message = "Stadium name must not be blank")
        String stadiumName,

        @NotBlank(message = "City must not be blank")
        String city,

        Integer foundedYear
) {}
