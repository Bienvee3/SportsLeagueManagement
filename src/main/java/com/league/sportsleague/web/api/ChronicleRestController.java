package com.league.sportsleague.web.api;

import com.league.sportsleague.application.service.ChronicleService;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/{id}/chronicle")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICIAL')")
    @Operation(summary = "Generate AI match chronicle (OFFICIAL). lang=ENGLISH (default) or CROATIAN.")
    public ChronicleResult generateChronicle(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "ENGLISH") Language lang) {
        return chronicleService.generateChronicle(id, lang);
    }
}
