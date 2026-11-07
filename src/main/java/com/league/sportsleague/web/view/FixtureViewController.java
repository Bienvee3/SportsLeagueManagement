package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("currentPage", "calendar");
        return "fixtures/detail";
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
        fixtureService.recordMatchEvent(id, form);
        ra.addFlashAttribute("flashKey", "fixtures.flash.eventRecorded");
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
