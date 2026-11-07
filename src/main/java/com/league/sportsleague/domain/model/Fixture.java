package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @OneToMany(mappedBy = "fixture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MatchEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "fixture", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ResultAudit> audits = new ArrayList<>();

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

    // Bidirectional helper methods
    public void addMatchEvent(MatchEvent event) {
        events.add(event);
        event.setFixture(this);
        if (isGoalEvent(event.getEventType())) {
            syncScoreFromEvents();
        }
    }

    public void removeMatchEvent(MatchEvent event) {
        events.remove(event);
        event.setFixture(null);
        if (isGoalEvent(event.getEventType())) {
            syncScoreFromEvents();
        }
    }

    private boolean isGoalEvent(MatchEventType type) {
        return type == MatchEventType.GOAL
                || type == MatchEventType.PENALTY
                || type == MatchEventType.OWN_GOAL;
    }

    public void syncScoreFromEvents() {
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
