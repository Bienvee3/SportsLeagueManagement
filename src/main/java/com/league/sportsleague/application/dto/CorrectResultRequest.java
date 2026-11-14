package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CorrectResultRequest(
        @Min(value = 0, message = "{validation.result.newHome.min}")
        int newHomeScore,

        @Min(value = 0, message = "{validation.result.newAway.min}")
        int newAwayScore,

        @NotBlank(message = "{validation.result.reason.required}")
        String reason
) {}
