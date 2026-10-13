package com.league.sportsleague.domain;

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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fixtures")
public class Fixture {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
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

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FixtureStatus status = FixtureStatus.SCHEDULED;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    // Optional attribute / relation required by LO4
    @Column(name = "postponement_reason", length = 255)
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

    // Bidirectional helper methods
    public void addMatchEvent(MatchEvent event) {
        events.add(event);
        event.setFixture(this);
    }

    public void removeMatchEvent(MatchEvent event) {
        events.remove(event);
        event.setFixture(null);
    }

    public void addResultAudit(ResultAudit audit) {
        audits.add(audit);
        audit.setFixture(this);
    }

    // Domain behavior
    public void recordResult(int homeScore, int awayScore) {
        this.homeScore = homeScore;
        this.awayScore = awayScore;
        this.status = FixtureStatus.FINISHED;
    }

    public void postpone(String reason) {
        this.status = FixtureStatus.POSTPONED;
        this.postponementReason = reason;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Round getRound() {
        return round;
    }

    public void setRound(Round round) {
        this.round = round;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public Team getAwayTeam() {
        return awayTeam;
    }

    public void setAwayTeam(Team awayTeam) {
        this.awayTeam = awayTeam;
    }

    public Integer getHomeScore() {
        return homeScore;
    }

    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    public Integer getAwayScore() {
        return awayScore;
    }

    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    public FixtureStatus getStatus() {
        return status;
    }

    public void setStatus(FixtureStatus status) {
        this.status = status;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getPostponementReason() {
        return postponementReason;
    }

    public void setPostponementReason(String postponementReason) {
        this.postponementReason = postponementReason;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public void setEvents(List<MatchEvent> events) {
        this.events = events;
    }

    public List<ResultAudit> getAudits() {
        return audits;
    }

    public void setAudits(List<ResultAudit> audits) {
        this.audits = audits;
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
