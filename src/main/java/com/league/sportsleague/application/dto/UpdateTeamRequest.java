package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTeamRequest(
        @NotBlank(message = "{validation.team.name.required}")
        String name,

        @NotBlank(message = "{validation.team.stadium.required}")
        String stadiumName,

        @NotBlank(message = "{validation.team.city.required}")
        String city,

        Integer foundedYear
) {}
