package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;

public record RecordResultRequest(
        @Min(value = 0, message = "Home score must be >= 0")
        int homeScore,

        @Min(value = 0, message = "Away score must be >= 0")
        int awayScore
) {}
