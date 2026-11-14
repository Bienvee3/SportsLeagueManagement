package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;

public record RecordResultRequest(
        @Min(value = 0, message = "{validation.result.home.min}")
        int homeScore,

        @Min(value = 0, message = "{validation.result.away.min}")
        int awayScore
) {}
