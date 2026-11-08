package com.league.sportsleague.web.api;

import com.league.sportsleague.application.service.ChronicleService;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/fixtures")
@Tag(name = "Fixtures", description = "Manage fixtures and match events")
public class ChronicleRestController {

    private final ChronicleService chronicleService;

    public ChronicleRestController(ChronicleService chronicleService) {
        this.chronicleService = chronicleService;
    }

    @GetMapping("/{id}/chronicle")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL','VIEWER')")
    @Operation(summary = "Get the stored chronicle in the requested language (any role). Read-only: never calls "
            + "the AI provider. 204 when none is stored in that language.")
    public ResponseEntity<ChronicleResult> getChronicle(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "ENGLISH") Language lang) {
        return chronicleService.findChronicle(id, lang)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/chronicle")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Generate a new AI match chronicle (ADMIN/OFFICIAL; paid provider call, replaces the "
            + "stored versions). lang=ENGLISH (default) or CROATIAN.")
    public ChronicleResult generateChronicle(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "ENGLISH") Language lang) {
        return chronicleService.generateChronicle(id, lang);
    }

    @PostMapping("/{id}/chronicle/translate")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Translate the stored chronicle into lang (ADMIN/OFFICIAL; paid provider call, stores "
            + "the translation). 204 when no chronicle exists yet.")
    public ResponseEntity<ChronicleResult> translateChronicle(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "ENGLISH") Language lang) {
        return chronicleService.translateChronicle(id, lang)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
