package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.service.FixtureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fixtures")
@Tag(name = "Fixtures", description = "Manage fixtures and match events")
public class FixtureRestController {

    private final FixtureService fixtureService;

    public FixtureRestController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get fixture by ID")
    public FixtureResponse getById(@PathVariable UUID id) {
        return fixtureService.findById(id);
    }

    @GetMapping("/by-round/{roundId}")
    @Operation(summary = "List fixtures by round")
    public List<FixtureResponse> byRound(@PathVariable UUID roundId) {
        return fixtureService.findByRound(roundId);
    }

    @GetMapping("/by-season/{seasonId}")
    @Operation(summary = "List fixtures by season")
    public List<FixtureResponse> bySeason(@PathVariable UUID seasonId) {
        return fixtureService.findBySeasonWithTeams(seasonId);
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Record match result (OFFICIAL)")
    public ResponseEntity<Void> recordResult(@PathVariable UUID id,
                                             @Valid @RequestBody RecordResultRequest request) {
        fixtureService.recordResult(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/correct-result")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Correct a recorded result with audit (OFFICIAL)")
    public ResponseEntity<Void> correctResult(@PathVariable UUID id,
                                              @Valid @RequestBody CorrectResultRequest request,
                                              @AuthenticationPrincipal UserDetails user) {
        fixtureService.correctResult(id, request, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/postpone")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Postpone a fixture (OFFICIAL)")
    public ResponseEntity<Void> postpone(@PathVariable UUID id,
                                         @RequestParam String reason) {
        fixtureService.postponeFixture(id, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Cancel a fixture (OFFICIAL)")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        fixtureService.cancelFixture(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Reschedule a postponed fixture (OFFICIAL)")
    public ResponseEntity<Void> reschedule(@PathVariable UUID id,
                                           @RequestParam Instant newTime) {
        fixtureService.rescheduleFixture(id, newTime);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Record a match event (OFFICIAL)")
    public ResponseEntity<Void> recordEvent(@PathVariable UUID id,
                                            @Valid @RequestBody RecordMatchEventRequest request) {
        UUID eventId = fixtureService.recordMatchEvent(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{eventId}").buildAndExpand(eventId).toUri();
        return ResponseEntity.created(location).build();
    }
}
