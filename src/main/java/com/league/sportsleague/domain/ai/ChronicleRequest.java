package com.league.sportsleague.domain.ai;

import java.util.List;

/**
 * Everything the AI may use to write a chronicle: the teams, the final score, the events in order and the
 * effect of the result on the league table. Nothing else — the text must not contain a fact that is not here.
 *
 * @param tableEffects effect on the table for the home and the away team (empty when unknown)
 * @param corrections  reasons why a previous answer was rejected; empty on the first attempt
 */
public record ChronicleRequest(
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        List<MatchEventSummary> events,
        Language language,
        List<TeamTableEffect> tableEffects,
        List<String> corrections
) {
    /** The same request, asking the model to fix the given problems of its previous answer. */
    public ChronicleRequest withCorrections(List<String> problems) {
        return new ChronicleRequest(homeTeam, awayTeam, homeScore, awayScore, events, language, tableEffects, List.copyOf(problems));
    }
}
