package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSeasonRequest(
        @NotBlank(message = "Season name must not be blank")
        String name,

        @Min(value = 1900, message = "Year start must be >= 1900")
        int yearStart,

        @Min(value = 1900, message = "Year end must be >= 1900")
        int yearEnd
) {}
