package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CorrectResultRequest(
        @Min(value = 0, message = "New home score must be >= 0")
        int newHomeScore,

        @Min(value = 0, message = "New away score must be >= 0")
        int newAwayScore,

        @NotBlank(message = "Correction reason must not be blank")
        String reason
) {}
