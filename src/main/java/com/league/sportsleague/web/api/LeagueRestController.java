package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.LeagueResponse;
import com.league.sportsleague.application.dto.UpdateLeagueRequest;
import com.league.sportsleague.application.service.LeagueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues")
@Tag(name = "Leagues", description = "Manage sports leagues")
public class LeagueRestController {

    private final LeagueService leagueService;

    public LeagueRestController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @GetMapping
    @Operation(summary = "List all leagues")
    public List<LeagueResponse> listAll() {
        return leagueService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get league by ID")
    public LeagueResponse getById(@PathVariable UUID id) {
        return leagueService.findById(id);
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get league by slug")
    public LeagueResponse getBySlug(@PathVariable String slug) {
        return leagueService.findBySlug(slug);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a league (ADMIN)")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateLeagueRequest request) {
        UUID id = leagueService.createLeague(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a league (ADMIN)")
    public ResponseEntity<Void> update(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateLeagueRequest request) {
        leagueService.rename(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a league (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leagueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
