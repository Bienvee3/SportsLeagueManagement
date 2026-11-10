package com.league.sportsleague.web.api;

import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.PlayerResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.service.TeamService;
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
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "Manage teams")
public class TeamRestController {

    private final TeamService teamService;

    public TeamRestController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @Operation(summary = "List all teams")
    public List<TeamResponse> listAll() {
        return teamService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID")
    public TeamResponse getById(@PathVariable UUID id) {
        return teamService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a team (ADMIN)")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateTeamRequest request) {
        UUID id = teamService.createTeam(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a team (ADMIN)")
    public ResponseEntity<Void> update(@PathVariable UUID id,
                                       @Valid @RequestBody CreateTeamRequest request) {
        teamService.updateProfile(id, request.name(), request.stadiumName(),
                request.city(), request.foundedYear());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/players")
    @Operation(summary = "List the squad of a team, ordered by shirt number")
    public List<PlayerResponse> listPlayers(@PathVariable UUID id) {
        return teamService.findPlayers(id);
    }

    @PostMapping("/{id}/players")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a player to the squad (ADMIN)")
    public ResponseEntity<Void> addPlayer(@PathVariable UUID id,
                                          @Valid @RequestBody PlayerRequest request) {
        UUID playerId = teamService.addPlayer(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{playerId}").buildAndExpand(playerId).toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{id}/players/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a player (ADMIN)")
    public ResponseEntity<Void> updatePlayer(@PathVariable UUID id,
                                             @PathVariable UUID playerId,
                                             @Valid @RequestBody PlayerRequest request) {
        teamService.updatePlayer(id, playerId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/players/{playerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a player from the squad; match history is kept (ADMIN)")
    public ResponseEntity<Void> removePlayer(@PathVariable UUID id, @PathVariable UUID playerId) {
        teamService.removePlayer(id, playerId);
        return ResponseEntity.noContent().build();
    }
}
