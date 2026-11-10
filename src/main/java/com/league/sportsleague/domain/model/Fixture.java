package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.LineupRules;
import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.model.enums.LineupRole;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.valueobject.Score;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fixtures")
public class Fixture {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_team_id", nullable = false)
    private Team awayTeam;

    private Integer homeScore;

    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    private FixtureStatus status = FixtureStatus.SCHEDULED;

    private Instant scheduledTime;

    // Optional attribute required by LO4
    private String postponementReason;

    // Batch-fetched: season-wide statistics and suspension checks walk the events and lineups of many
    // fixtures, and two bags cannot be join-fetched in one query.
    @OneToMany(mappedBy = "fixture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<MatchEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "fixture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ResultAudit> audits = new ArrayList<>();

    @OneToMany(mappedBy = "fixture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<LineupEntry> lineupEntries = new ArrayList<>();

    public Fixture() {
        this.id = UUID.randomUUID();
    }

    public Fixture(Round round, Team homeTeam, Team awayTeam, Instant scheduledTime) {
        this();
        Objects.requireNonNull(round, "Round must not be null");
        Objects.requireNonNull(homeTeam, "Home team must not be null");
        Objects.requireNonNull(awayTeam, "Away team must not be null");
        Objects.requireNonNull(scheduledTime, "Scheduled time must not be null");
        if (homeTeam.equals(awayTeam)) {
            throw new DuplicateTeamException(
                homeTeam.getShortCode().value(), "both home and away in the same fixture");
        }
        this.round = round;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.scheduledTime = scheduledTime;
        this.status = FixtureStatus.SCHEDULED;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    // Domain Business Methods

    /**
     * Starts the match. Fixture must be SCHEDULED. The score starts at 0-0; a fixture that already
     * carries a score synced from earlier goal events keeps it.
     */
    public void kickOff() {
        if (this.status != FixtureStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "kickOff");
        }
        this.status = FixtureStatus.IN_PROGRESS;
        if (this.homeScore == null) this.homeScore = 0;
        if (this.awayScore == null) this.awayScore = 0;
    }

    /** Ends a match in progress with the score reached through its goal events. */
    public void finish() {
        if (this.status != FixtureStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "finish");
        }
        this.status = FixtureStatus.FINISHED;
        if (this.homeScore == null) this.homeScore = 0;
        if (this.awayScore == null) this.awayScore = 0;
        this.postponementReason = null;
    }

    /** Records the final result. Fixture must be SCHEDULED or IN_PROGRESS. */
    public void recordResult(Score score) {
        Objects.requireNonNull(score, "Score must not be null");
        if (this.status != FixtureStatus.SCHEDULED && this.status != FixtureStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "recordResult");
        }
        this.homeScore = score.home();
        this.awayScore = score.away();
        this.status = FixtureStatus.FINISHED;
        this.postponementReason = null;
    }

    /**
     * Corrects the result of a FINISHED fixture. Creates an immutable ResultAudit
     * capturing the old and new scores, then updates this fixture's score.
     * The audit is persisted via cascade when this fixture is saved.
     */
    public void correctResult(Score newScore, String reason, String correctedBy) {
        Objects.requireNonNull(newScore, "New score must not be null");
        Objects.requireNonNull(reason, "Correction reason must not be null");
        Objects.requireNonNull(correctedBy, "Corrected-by must not be null");
        if (this.status != FixtureStatus.FINISHED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "correctResult");
        }
        ResultAudit audit = new ResultAudit(this, this.homeScore, this.awayScore,
                                            newScore.home(), newScore.away(), reason, correctedBy);
        this.audits.add(audit);
        audit.setFixture(this);
        this.homeScore = newScore.home();
        this.awayScore = newScore.away();
    }

    // Domain Queries
    public boolean isFinished() { return this.status == FixtureStatus.FINISHED; }

    public boolean isDraw() {
        return isFinished() && this.homeScore != null && this.awayScore != null
               && this.homeScore.equals(this.awayScore);
    }

    /** Postpones a SCHEDULED or IN_PROGRESS fixture. */
    public void postpone(String reason) {
        if (this.status != FixtureStatus.SCHEDULED && this.status != FixtureStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "postpone");
        }
        this.status = FixtureStatus.POSTPONED;
        this.postponementReason = reason;
    }

    /** Cancels the fixture. Cannot cancel a FINISHED or already CANCELLED fixture. */
    public void cancel() {
        if (this.status == FixtureStatus.FINISHED || this.status == FixtureStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "cancel");
        }
        this.status = FixtureStatus.CANCELLED;
    }

    /** Reschedules a POSTPONED fixture back to SCHEDULED. */
    public void reschedule(Instant newTime) {
        if (this.status != FixtureStatus.POSTPONED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "reschedule");
        }
        this.scheduledTime = newTime;
        this.status = FixtureStatus.SCHEDULED;
        this.postponementReason = null;
    }

    // Lineups (match sheet)

    /** The match-sheet rules of this fixture, taken from the sport of its teams. */
    public LineupRules getLineupRules() {
        return LineupRules.forSport(this.homeTeam.getSportType());
    }

    /**
     * Files or replaces the lineup of one team. Only possible before kick-off (SCHEDULED); the number of
     * starters and the bench size follow the sport ({@link LineupRules}).
     *
     * @param captain one of the starters, or null when no captain is named
     * @throws InvalidStateTransitionException once the fixture is no longer SCHEDULED
     * @throws IllegalArgumentException        for a team that does not play here, a wrong number of players,
     *                                         a player listed twice, a player who is not an active member
     *                                         of the team, or a captain who does not start
     * @throws IllegalStateException           if events of this team already reference its players
     */
    public void submitLineup(Team team, List<Player> starters, List<Player> bench, Player captain) {
        Objects.requireNonNull(starters, "Starters must not be null");
        Objects.requireNonNull(bench, "Bench must not be null");
        requireLineupEditable(team, "submitLineup");
        LineupRules rules = getLineupRules();
        if (starters.size() != rules.starters()) {
            throw new IllegalArgumentException(
                    "Exactly %d starters are required, got %d".formatted(rules.starters(), starters.size()));
        }
        if (bench.size() > rules.maxBench()) {
            throw new IllegalArgumentException(
                    "At most %d substitutes are allowed, got %d".formatted(rules.maxBench(), bench.size()));
        }
        Map<UUID, LineupRole> roles = new LinkedHashMap<>();
        Map<UUID, Player> selected = new LinkedHashMap<>();
        registerSelection(starters, LineupRole.STARTER, roles, selected);
        registerSelection(bench, LineupRole.SUBSTITUTE, roles, selected);
        for (Player player : selected.values()) {
            if (!player.isActive() || !isSameTeam(player.getTeam(), team)) {
                throw new IllegalArgumentException(
                        "%s is not an active player of %s".formatted(player.getName(), team.getName()));
            }
        }
        UUID captainId = captain == null ? null : captain.getId();
        if (captainId != null && roles.get(captainId) != LineupRole.STARTER) {
            throw new IllegalArgumentException("The captain must be one of the starters");
        }

        // Applied as a diff: an entry of a player who stays is updated in place. Deleting and re-inserting
        // it would break uk_lineup_entries_fixture_player, because Hibernate flushes inserts before deletes.
        for (LineupEntry entry : getLineup(team)) {
            UUID playerId = entry.getPlayer().getId();
            if (selected.remove(playerId) != null) {
                entry.reassign(roles.get(playerId), playerId.equals(captainId));
            } else {
                lineupEntries.remove(entry);
                entry.setFixture(null);
            }
        }
        for (Player player : selected.values()) {
            lineupEntries.add(new LineupEntry(this, player, roles.get(player.getId()),
                    player.getId().equals(captainId)));
        }
    }

    /** Withdraws the lineup of one team. Same conditions as {@link #submitLineup}. */
    public void clearLineup(Team team) {
        requireLineupEditable(team, "clearLineup");
        for (LineupEntry entry : getLineup(team)) {
            lineupEntries.remove(entry);
            entry.setFixture(null);
        }
    }

    private void requireLineupEditable(Team team, String operation) {
        Objects.requireNonNull(team, "Team must not be null");
        if (this.status != FixtureStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), operation);
        }
        if (!isSameTeam(team, this.homeTeam) && !isSameTeam(team, this.awayTeam)) {
            throw new IllegalArgumentException("Team does not play in this fixture");
        }
        boolean hasPlayerEvents = events.stream()
                .anyMatch(e -> e.getPlayer() != null && isSameTeam(e.getTeam(), team));
        if (hasPlayerEvents) {
            throw new IllegalStateException("The lineup cannot change: events are already recorded for this team");
        }
    }

    private void registerSelection(List<Player> players, LineupRole role,
                                   Map<UUID, LineupRole> roles, Map<UUID, Player> selected) {
        for (Player player : players) {
            Objects.requireNonNull(player, "Player must not be null");
            if (roles.putIfAbsent(player.getId(), role) != null) {
                throw new IllegalArgumentException("%s is listed more than once".formatted(player.getName()));
            }
            selected.put(player.getId(), player);
        }
    }

    /** The lineup entries (starters and substitutes) of one team; empty when none has been filed. */
    public List<LineupEntry> getLineup(Team team) {
        return lineupEntries.stream()
                .filter(entry -> isSameTeam(entry.getTeam(), team))
                .toList();
    }

    public boolean hasLineup(Team team) {
        return lineupEntries.stream().anyMatch(entry -> isSameTeam(entry.getTeam(), team));
    }

    // Match events

    /**
     * Adds a match event. No event is accepted before kick-off (SCHEDULED). Goal events (GOAL, PENALTY,
     * OWN_GOAL) are only accepted while the fixture is IN_PROGRESS and keep the score in sync with the
     * goals; once the fixture is FINISHED the only audited way to change the score is
     * {@link #correctResult}. Other events (cards, substitutions) never touch the score and stay allowed
     * in every other status.
     *
     * <p>If the event's team filed a lineup, the event must reference players of that lineup. Without a
     * lineup an event may still carry a free-text player name.
     *
     * @throws InvalidStateTransitionException for any event on a SCHEDULED fixture and for a goal event
     *                                         on a FINISHED, POSTPONED or CANCELLED fixture
     * @throws IllegalArgumentException        if the event's team is neither the home nor the away team,
     *                                         or its players do not fit the team or its lineup
     */
    public void addMatchEvent(MatchEvent event) {
        Objects.requireNonNull(event, "Match event must not be null");
        if (this.status == FixtureStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Fixture", status.name(), "addMatchEvent");
        }
        boolean goal = isGoalEvent(event.getEventType());
        if (goal) {
            requireGoalEventsAllowed("addGoalEvent");
        }
        if (!isSameTeam(event.getTeam(), this.homeTeam) && !isSameTeam(event.getTeam(), this.awayTeam)) {
            throw new IllegalArgumentException("Team of the event does not play in this fixture");
        }
        requirePlayersFitTeam(event);
        events.add(event);
        event.setFixture(this);
        if (goal) {
            syncScoreFromEvents();
        }
    }

    /** Removes a match event. Removing a goal event follows the same status rule as adding one. */
    public void removeMatchEvent(MatchEvent event) {
        boolean goal = isGoalEvent(event.getEventType());
        if (goal) {
            requireGoalEventsAllowed("removeGoalEvent");
        }
        events.remove(event);
        event.setFixture(null);
        if (goal) {
            syncScoreFromEvents();
        }
    }

    private void requireGoalEventsAllowed(String operation) {
        if (this.status != FixtureStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Fixture", status.name(), operation);
        }
    }

    private void requirePlayersFitTeam(MatchEvent event) {
        Team team = event.getTeam();
        List<LineupEntry> lineup = getLineup(team);
        if (!lineup.isEmpty()) {
            if (event.getPlayer() == null) {
                throw new IllegalArgumentException(
                        "A lineup is registered for %s: select the player from it".formatted(team.getName()));
            }
            requireInLineup(lineup, event.getPlayer());
            if (event.getRelatedPlayer() != null) {
                requireInLineup(lineup, event.getRelatedPlayer());
            }
            return;
        }
        for (Player player : new Player[] {event.getPlayer(), event.getRelatedPlayer()}) {
            if (player != null && !isSameTeam(player.getTeam(), team)) {
                throw new IllegalArgumentException(
                        "%s does not play for %s".formatted(player.getName(), team.getName()));
            }
        }
    }

    private void requireInLineup(List<LineupEntry> lineup, Player player) {
        boolean listed = lineup.stream().anyMatch(entry -> entry.getPlayer().getId().equals(player.getId()));
        if (!listed) {
            throw new IllegalArgumentException("%s is not in the lineup of this match".formatted(player.getName()));
        }
    }

    private boolean isGoalEvent(MatchEventType type) {
        return type == MatchEventType.GOAL
                || type == MatchEventType.PENALTY
                || type == MatchEventType.OWN_GOAL;
    }

    private void syncScoreFromEvents() {
        int homeGoals = 0;
        int awayGoals = 0;
        for (MatchEvent event : this.events) {
            if (isGoalEvent(event.getEventType())) {
                if (isSameTeam(event.getTeam(), this.homeTeam)) {
                    if (event.getEventType() == MatchEventType.OWN_GOAL) {
                        awayGoals++;
                    } else {
                        homeGoals++;
                    }
                } else if (isSameTeam(event.getTeam(), this.awayTeam)) {
                    if (event.getEventType() == MatchEventType.OWN_GOAL) {
                        homeGoals++;
                    } else {
                        awayGoals++;
                    }
                }
            }
        }
        this.homeScore = homeGoals;
        this.awayScore = awayGoals;
    }

    private boolean isSameTeam(Team a, Team b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.equals(b);
    }

    public void addResultAudit(ResultAudit audit) {
        audits.add(audit);
        audit.setFixture(this);
    }

    void setRound(Round round) {
        this.round = round;
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public Round getRound() {
        return round;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public FixtureStatus getStatus() {
        return status;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public String getPostponementReason() {
        return postponementReason;
    }

    public List<MatchEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public List<ResultAudit> getAudits() {
        return Collections.unmodifiableList(audits);
    }

    public List<LineupEntry> getLineupEntries() {
        return Collections.unmodifiableList(lineupEntries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fixture other)) return false;
        return Objects.equals(round != null ? round.getId() : null,
                              other.round != null ? other.round.getId() : null) &&
               Objects.equals(homeTeam != null ? homeTeam.getShortCode() : null,
                              other.homeTeam != null ? other.homeTeam.getShortCode() : null) &&
               Objects.equals(awayTeam != null ? awayTeam.getShortCode() : null,
                              other.awayTeam != null ? other.awayTeam.getShortCode() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(round != null ? round.getId() : null,
                            homeTeam != null ? homeTeam.getShortCode() : null,
                            awayTeam != null ? awayTeam.getShortCode() : null);
    }

    @Override
    public String toString() {
        return "Fixture{" +
                "id=" + id +
                ", homeScore=" + homeScore +
                ", awayScore=" + awayScore +
                ", status=" + status +
                ", scheduledTime=" + scheduledTime +
                '}';
    }
}
