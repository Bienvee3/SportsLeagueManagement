package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.ai.MatchEventSummary;
import com.league.sportsleague.domain.ai.TeamTableEffect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The prompt is the only place the model learns the facts, so what goes into it is pinned here. */
class GeminiPromptTest {

    private static ChronicleRequest request(List<TeamTableEffect> effects, List<String> corrections) {
        return new ChronicleRequest("Dinamo Zagreb", "Hajduk Split", 2, 1,
                List.of(new MatchEventSummary(23, "GOAL", "Dinamo Zagreb", "Petkovic")),
                Language.CROATIAN, effects, corrections);
    }

    @Test
    @DisplayName("the prompt carries teams, final score, events, language and the clean-output rules")
    void carriesTheMatchData() {
        String prompt = GeminiChronicleAdapter.buildPrompt(request(List.of(), List.of()));

        assertTrue(prompt.contains("Home team : Dinamo Zagreb"));
        assertTrue(prompt.contains("Final score: 2-1"));
        assertTrue(prompt.contains("23' — Goal — Dinamo Zagreb (Petkovic)"));
        assertTrue(prompt.contains("Croatian (Hrvatski) only"));
        assertTrue(prompt.contains("home team's goals first"));
        assertTrue(prompt.contains("no word count, no notes"));
        assertFalse(prompt.contains("EFFECT ON THE LEAGUE TABLE"), "no table section without table data");
        assertFalse(prompt.contains("REJECTED"), "no correction section on the first attempt");
    }

    @Test
    @DisplayName("the effect on the table is written with positions and points before and after")
    void carriesTheTableEffect() {
        String prompt = GeminiChronicleAdapter.buildPrompt(request(List.of(
                new TeamTableEffect("Dinamo Zagreb", 2, 1, 0, 3),
                new TeamTableEffect("Hajduk Split", null, 2, 0, 0)), List.of()));

        assertTrue(prompt.contains("Dinamo Zagreb: position 2 with 0 points -> position 1 with 3 points"));
        assertTrue(prompt.contains("Hajduk Split: first match of the season -> position 2 with 0 points"));
        assertTrue(prompt.contains("what the result meant for the table"));
    }

    @Test
    @DisplayName("a retry lists the reasons the previous answer was rejected")
    void carriesTheCorrections() {
        String prompt = GeminiChronicleAdapter.buildPrompt(request(List.of(),
                List.of("The scoreline 5-0 is not in the data")));

        assertTrue(prompt.contains("YOUR PREVIOUS ANSWER WAS REJECTED"));
        assertTrue(prompt.contains("- The scoreline 5-0 is not in the data"));
    }
}
