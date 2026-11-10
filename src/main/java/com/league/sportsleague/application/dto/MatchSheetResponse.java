package com.league.sportsleague.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * The match sheet of a fixture: the lineup of both teams plus what is needed to edit it
 * (lineup sizes of the sport and the selectable squad).
 */
public record MatchSheetResponse(
        int startersRequired,
        int maxBench,
        TeamSheet home,
        TeamSheet away
) {
    /**
     * @param starters  filed starters, by shirt number
     * @param bench     filed substitutes, by shirt number
     * @param squad     active players of the team with their current selection, by shirt number
     * @param suspended squad players who may not be selected for this fixture
     */
    public record TeamSheet(
            UUID teamId,
            String teamName,
            boolean lineupSubmitted,
            List<LineupPlayer> starters,
            List<LineupPlayer> bench,
            List<SquadOption> squad,
            List<SquadOption> suspended
    ) {}

    public record LineupPlayer(
            UUID playerId,
            int shirtNumber,
            String name,
            String position,
            boolean captain
    ) {}

    /** selectedRole: "STARTER", "SUBSTITUTE" or null. suspensionReason: SuspensionReason name or null. */
    public record SquadOption(
            UUID playerId,
            int shirtNumber,
            String name,
            String position,
            String selectedRole,
            boolean captain,
            String suspensionReason
    ) {}
}
