package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.LineupEntry;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.enums.MatchEventType;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Who is on the pitch, minute by minute, for one team in one match. Built by replaying the team's
 * lineup and its substitutions and red cards in minute order. Pure domain logic: no JPA, no Spring.
 *
 * <p>The replay validates every substitution and red card, so a fixture can use it to reject an event
 * that would make the match history impossible (a player coming on twice, a substitution beyond the
 * limit of the sport, a sent-off player coming back...).
 */
public final class PitchTimeline {

    private final Map<UUID, Integer> onPitchSince = new HashMap<>();
    private final Map<UUID, Integer> minutesInClosedSpells = new HashMap<>();
    private final Set<UUID> appeared = new LinkedHashSet<>();
    private final Set<UUID> sentOff = new HashSet<>();

    private PitchTimeline() {
    }

    /**
     * Replays a team's match: the starters are on the pitch from minute 0, then every SUBSTITUTION and
     * RED_CARD that references a squad player is applied in minute order (events of the same minute in
     * recording order). Other events are ignored.
     *
     * @param teamLineup the lineup entries of the team
     * @param teamEvents the match events of the same team
     * @throws IllegalArgumentException if a substitution or red card is not possible at its minute
     */
    public static PitchTimeline replay(List<LineupEntry> teamLineup, List<MatchEvent> teamEvents, LineupRules rules) {
        PitchTimeline timeline = new PitchTimeline();
        for (LineupEntry entry : teamLineup) {
            if (entry.isStarter()) {
                timeline.enter(entry.getPlayer().getId(), 0);
            }
        }
        List<MatchEvent> relevant = teamEvents.stream()
                .filter(e -> e.getPlayer() != null)
                .filter(e -> e.getEventType() == MatchEventType.SUBSTITUTION
                          || e.getEventType() == MatchEventType.RED_CARD)
                .sorted(Comparator.<MatchEvent>comparingInt(e -> e.getMatchMinute().value())
                        .thenComparing(MatchEvent::getCreatedAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder())))
                .toList();
        int substitutions = 0;
        for (MatchEvent event : relevant) {
            int minute = event.getMatchMinute().value();
            if (event.getEventType() == MatchEventType.SUBSTITUTION) {
                timeline.substitute(event.getPlayer(), event.getRelatedPlayer(), minute, substitutions, rules);
                substitutions++;
            } else {
                timeline.sendOff(event.getPlayer(), minute);
            }
        }
        return timeline;
    }

    private void substitute(Player off, Player on, int minute, int substitutionsMade, LineupRules rules) {
        if (on == null) {
            throw new IllegalArgumentException("A substitution needs the player coming on");
        }
        if (!onPitchSince.containsKey(off.getId())) {
            throw new IllegalArgumentException("%s is not on the pitch at minute %d".formatted(off.getName(), minute));
        }
        if (onPitchSince.containsKey(on.getId())) {
            throw new IllegalArgumentException("%s is already on the pitch at minute %d".formatted(on.getName(), minute));
        }
        if (sentOff.contains(on.getId())) {
            throw new IllegalArgumentException("%s was sent off and cannot come on".formatted(on.getName()));
        }
        if (!rules.reentryAllowed() && appeared.contains(on.getId())) {
            throw new IllegalArgumentException(
                    "%s has already played in this match and cannot re-enter".formatted(on.getName()));
        }
        if (substitutionsMade >= rules.maxSubstitutions()) {
            throw new IllegalArgumentException("Substitution limit reached (%d)".formatted(rules.maxSubstitutions()));
        }
        leave(off.getId(), minute);
        enter(on.getId(), minute);
    }

    private void sendOff(Player player, int minute) {
        if (!sentOff.add(player.getId())) {
            throw new IllegalArgumentException("%s has already been sent off".formatted(player.getName()));
        }
        if (onPitchSince.containsKey(player.getId())) {
            leave(player.getId(), minute);
        }
    }

    private void enter(UUID playerId, int minute) {
        onPitchSince.put(playerId, minute);
        appeared.add(playerId);
    }

    private void leave(UUID playerId, int minute) {
        int since = onPitchSince.remove(playerId);
        minutesInClosedSpells.merge(playerId, minute - since, Integer::sum);
    }

    /** Everyone who was on the pitch at some point of the match (starters included). */
    public Set<UUID> appearedPlayerIds() {
        return Set.copyOf(appeared);
    }

    /**
     * Minutes the player spent on the pitch, counting a spell still open at the end as lasting until
     * {@code endMinute}. 0 for a player who never played.
     */
    public int minutesPlayed(UUID playerId, int endMinute) {
        int minutes = minutesInClosedSpells.getOrDefault(playerId, 0);
        Integer since = onPitchSince.get(playerId);
        if (since != null) {
            minutes += Math.max(endMinute - since, 0);
        }
        return minutes;
    }
}
