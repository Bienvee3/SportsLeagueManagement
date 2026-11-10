package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.dto.SubmitLineupRequest;
import com.league.sportsleague.application.service.FixtureService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/fixtures")
public class FixtureViewController {

    private final FixtureService fixtureService;

    public FixtureViewController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("fixture", fixtureService.findById(id));
        model.addAttribute("sheet", fixtureService.getMatchSheet(id));
        model.addAttribute("currentPage", "calendar");
        return "fixtures/detail";
    }

    @PostMapping("/{id}/kick-off")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String kickOff(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            fixtureService.kickOff(id);
            ra.addFlashAttribute("flashKey", "fixtures.flash.kickedOff");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String finish(@PathVariable UUID id, RedirectAttributes ra) {
        try {
            fixtureService.finish(id);
            ra.addFlashAttribute("flashKey", "fixtures.flash.finished");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String recordResult(@PathVariable UUID id,
                               @Valid RecordResultRequest form,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid input: " + br.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/fixtures/" + id;
        }
        fixtureService.recordResult(id, form);
        ra.addFlashAttribute("flashKey", "fixtures.flash.resultRecorded");
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/correct-result")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String correctResult(@PathVariable UUID id,
                                @Valid CorrectResultRequest form,
                                BindingResult br,
                                @AuthenticationPrincipal UserDetails user,
                                RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid input: " + br.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/fixtures/" + id;
        }
        fixtureService.correctResult(id, form, user.getUsername());
        ra.addFlashAttribute("flashKey", "fixtures.flash.resultCorrected");
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String recordEvent(@PathVariable UUID id,
                              @Valid RecordMatchEventRequest form,
                              BindingResult br,
                              RedirectAttributes ra) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("errorMsg", "Invalid input: " + br.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/fixtures/" + id;
        }
        try {
            fixtureService.recordMatchEvent(id, form);
            ra.addFlashAttribute("flashKey", "fixtures.flash.eventRecorded");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/events/{eventId}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String deleteEvent(@PathVariable UUID id,
                              @PathVariable UUID eventId,
                              RedirectAttributes ra) {
        try {
            fixtureService.deleteMatchEvent(id, eventId);
            ra.addFlashAttribute("flashKey", "fixtures.flash.eventDeleted");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    // ── Match sheet: lineups ──────────────────────────────────────────────────

    @PostMapping("/{id}/lineups/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String submitLineup(@PathVariable UUID id,
                               @PathVariable UUID teamId,
                               @RequestParam(required = false) List<UUID> starterIds,
                               @RequestParam(required = false) List<UUID> benchIds,
                               @RequestParam(required = false) UUID captainId,
                               RedirectAttributes ra) {
        try {
            fixtureService.submitLineup(id, teamId, new SubmitLineupRequest(starterIds, benchIds, captainId));
            ra.addFlashAttribute("flashKey", "fixtures.flash.lineupSaved");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/lineups/{teamId}/clear")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String clearLineup(@PathVariable UUID id, @PathVariable UUID teamId, RedirectAttributes ra) {
        try {
            fixtureService.clearLineup(id, teamId);
            ra.addFlashAttribute("flashKey", "fixtures.flash.lineupCleared");
        } catch (IllegalStateException | IllegalArgumentException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/postpone")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String postpone(@PathVariable UUID id,
                           String reason,
                           RedirectAttributes ra) {
        fixtureService.postponeFixture(id, reason);
        ra.addFlashAttribute("flashKey", "fixtures.flash.postponed");
        return "redirect:/fixtures/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        fixtureService.cancelFixture(id);
        ra.addFlashAttribute("flashKey", "fixtures.flash.cancelled");
        return "redirect:/fixtures/" + id;
    }
}
