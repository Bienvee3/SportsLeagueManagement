package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterTeamRequest(
        @NotNull(message = "{validation.team.required}")
        UUID teamId,

        @Min(value = 0, message = "{validation.registration.seed.min}")
        int initialSeed
) {}
