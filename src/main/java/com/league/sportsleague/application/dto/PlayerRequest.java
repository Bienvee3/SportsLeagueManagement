package com.league.sportsleague.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerRequest(
        @NotBlank(message = "Player name must not be blank")
        @Size(max = 100, message = "Player name must be at most 100 characters")
        String name,

        @NotNull(message = "Shirt number is required")
        @Min(value = 1, message = "Shirt number must be >= 1")
        @Max(value = 99, message = "Shirt number must be <= 99")
        Integer shirtNumber,

        @Size(max = 30, message = "Position must be at most 30 characters")
        String position
) {}
