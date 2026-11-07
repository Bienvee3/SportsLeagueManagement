package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RegisterTeamRequest;
import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.application.service.FixtureService;
import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.application.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Controller
@RequestMapping("/leagues/{leagueId}/seasons")
public class SeasonViewController {

    private final SeasonService seasonService;
    private final LeagueService leagueService;
    private final TeamService teamService;
    private final FixtureService fixtureService;

    public SeasonViewController(SeasonService seasonService,
                                LeagueService leagueService,
                                TeamService teamService,
                                FixtureService fixtureService) {
        this.seasonService = seasonService;
        this.leagueService = leagueService;
        this.teamService = teamService;
        this.fixtureService = fixtureService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID leagueId,
                         @PathVariable UUID id,
                         Model model) {
        var season = seasonService.findById(id);
        var league = leagueService.findById(leagueId);
        var standings = seasonService.getStandings(id);
        var registeredTeams = seasonService.findRegisteredTeams(id);
        var allTeams        = teamService.findAll();
        var registeredIds   = seasonService.findAllSeasonTeamIds(id);
        var availableTeams  = allTeams.stream()
                .filter(t -> !registeredIds.contains(t.id()))
                .filter(t -> t.sportType().equalsIgnoreCase(league.sportType()))
                .toList();
        model.addAttribute("season", season);
        model.addAttribute("league", league);
        model.addAttribute("standings", standings);
        model.addAttribute("registeredTeams", registeredTeams);
        model.addAttribute("availableTeams", availableTeams);
        model.addAttribute("currentPage", "leagues");
        return "seasons/detail";
    }

    @GetMapping("/{id}/calendar")
    public String calendar(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           Model model) {
        var season = seasonService.findById(id);
        var fixtures = fixtureService.findAllBySeason(id);
        model.addAttribute("season", season);
        model.addAttribute("league", leagueService.findById(leagueId));
        model.addAttribute("fixtures", fixtures);
        model.addAttribute("rounds", seasonService.findRounds(id));
        model.addAttribute("fixturesByRound", fixtures.stream()
                .collect(java.util.stream.Collectors.groupingBy(FixtureResponse::roundId)));
        model.addAttribute("registeredTeams", seasonService.findRegisteredTeams(id));
        model.addAttribute("currentPage", "calendar");
        return "seasons/calendar";
    }

    // ── ADMIN: rounds and matches (one by one) ───────────────────────────────

    @PostMapping("/{id}/rounds")
    @PreAuthorize("hasRole('ADMIN')")
    public String createRound(@PathVariable UUID leagueId,
                              @PathVariable UUID id,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              RedirectAttributes ra) {
        try {
            seasonService.createRound(id, startDate);
            ra.addFlashAttribute("flashMsg", "Round created. Now add its matches.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    @PostMapping("/{id}/rounds/{roundId}/fixtures")
    @PreAuthorize("hasRole('ADMIN')")
    public String addFixture(@PathVariable UUID leagueId,
                             @PathVariable UUID id,
                             @PathVariable UUID roundId,
                             @RequestParam UUID homeTeamId,
                             @RequestParam UUID awayTeamId,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
                             RedirectAttributes ra) {
        try {
            seasonService.addFixture(id, roundId, homeTeamId, awayTeamId, scheduledTime.toInstant(ZoneOffset.UTC));
            ra.addFlashAttribute("flashMsg", "Match added.");
        } catch (IllegalStateException | IllegalArgumentException | DuplicateTeamException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    @PostMapping("/{id}/rounds/{roundId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRound(@PathVariable UUID leagueId,
                              @PathVariable UUID id,
                              @PathVariable UUID roundId,
                              RedirectAttributes ra) {
        try {
            seasonService.deleteRound(id, roundId);
            ra.addFlashAttribute("flashMsg", "Round deleted.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    @PostMapping("/{id}/rounds/{roundId}/fixtures/{fixtureId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteFixture(@PathVariable UUID leagueId,
                                @PathVariable UUID id,
                                @PathVariable UUID roundId,
                                @PathVariable UUID fixtureId,
                                RedirectAttributes ra) {
        try {
            seasonService.deleteFixture(id, roundId, fixtureId);
            ra.addFlashAttribute("flashMsg", "Match deleted.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    // ── ADMIN: create ─────────────────────────────────────────────────────────

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newForm(@PathVariable UUID leagueId, Model model) {
        model.addAttribute("league", leagueService.findById(leagueId));
        model.addAttribute("form", new CreateSeasonRequest("", 2026, 2027));
        model.addAttribute("currentPage", "leagues");
        return "seasons/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String create(@PathVariable UUID leagueId,
                         @ModelAttribute("form") @Valid CreateSeasonRequest form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("league", leagueService.findById(leagueId));
            model.addAttribute("form", form);
            model.addAttribute("currentPage", "leagues");
            return "seasons/form";
        }
        try {
            UUID id = seasonService.createSeason(leagueId, form);
            ra.addFlashAttribute("flashMsg", "Season created.");
            return "redirect:/leagues/" + leagueId + "/seasons/" + id;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("league", leagueService.findById(leagueId));
            model.addAttribute("form", form);
            model.addAttribute("currentPage", "leagues");
            model.addAttribute("errorMsg", "A season named \"" + form.name() + "\" already exists in this league.");
            return "seasons/form";
        } catch (Exception e) {
            model.addAttribute("league", leagueService.findById(leagueId));
            model.addAttribute("form", form);
            model.addAttribute("currentPage", "leagues");
            model.addAttribute("errorMsg", e.getMessage());
            return "seasons/form";
        }
    }

    // ── ADMIN: lifecycle ──────────────────────────────────────────────────────

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public String activate(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           RedirectAttributes ra) {
        seasonService.activateSeason(id);
        ra.addFlashAttribute("flashMsg", "Season activated.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public String complete(@PathVariable UUID leagueId,
                           @PathVariable UUID id,
                           RedirectAttributes ra) {
        seasonService.completeSeason(id);
        ra.addFlashAttribute("flashMsg", "Season completed.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID leagueId,
                         @PathVariable UUID id,
                         RedirectAttributes ra) {
        try {
            seasonService.deleteSeason(id);
            ra.addFlashAttribute("flashMsg", "Season deleted.");
            return "redirect:/leagues/" + leagueId;
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Cannot delete season: " + e.getMessage());
            return "redirect:/leagues/" + leagueId + "/seasons/" + id;
        }
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public String generateSchedule(@PathVariable UUID leagueId,
                                   @PathVariable UUID id,
                                   RedirectAttributes ra) {
        try {
            seasonService.generateSchedule(id, Instant.now());
            ra.addFlashAttribute("flashMsg", "Schedule generated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Could not generate schedule: " + e.getMessage());
        }
        return "redirect:/leagues/" + leagueId + "/seasons/" + id + "/calendar";
    }

    @PostMapping("/{id}/register-team")
    @PreAuthorize("hasRole('ADMIN')")
    public String registerTeam(@PathVariable UUID leagueId,
                               @PathVariable UUID id,
                               @Valid RegisterTeamRequest form,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid team selection");
            return "redirect:/leagues/" + leagueId + "/seasons/" + id;
        }
        seasonService.registerTeam(id, form.teamId(), form.initialSeed());
        ra.addFlashAttribute("flashMsg", "Team registered.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }

    @PostMapping("/{id}/withdraw-team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String withdrawTeam(@PathVariable UUID leagueId,
                               @PathVariable UUID id,
                               @PathVariable UUID teamId,
                               RedirectAttributes ra) {
        seasonService.withdrawTeam(id, teamId);
        ra.addFlashAttribute("flashMsg", "Team withdrawn.");
        return "redirect:/leagues/" + leagueId + "/seasons/" + id;
    }
}
