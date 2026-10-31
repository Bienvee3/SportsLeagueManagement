package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "seasons",
    uniqueConstraints = @UniqueConstraint(name = "uk_seasons_league_name", columnNames = {"league_id", "name"})
)
public class Season {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    private String name;

    private int yearStart;

    private int yearEnd;

    @Enumerated(EnumType.STRING)
    private SeasonStatus status = SeasonStatus.UPCOMING;

    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SeasonTeam> seasonTeams = new ArrayList<>();

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Round> rounds = new ArrayList<>();

    public Season() {
        this.id = UUID.randomUUID();
    }

    public Season(League league, String name, int yearStart, int yearEnd, SeasonStatus status) {
        this();
        Objects.requireNonNull(league, "League must not be null");
        Objects.requireNonNull(name, "Season name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Season name must not be blank");
        if (yearStart >= yearEnd)
            throw new IllegalArgumentException(
                "yearStart (%d) must be less than yearEnd (%d)".formatted(yearStart, yearEnd));
        this.league = league;
        this.name = name;
        this.yearStart = yearStart;
        this.yearEnd = yearEnd;
        this.status = (status != null) ? status : SeasonStatus.UPCOMING;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Domain State Transitions
    public void activate() {
        if (this.status != SeasonStatus.UPCOMING) {
            throw new InvalidStateTransitionException("Season", status.name(), "activate");
        }
        this.status = SeasonStatus.ACTIVE;
    }

    public void complete() {
        if (this.status != SeasonStatus.ACTIVE) {
            throw new InvalidStateTransitionException("Season", status.name(), "complete");
        }
        this.status = SeasonStatus.COMPLETED;
    }

    // Bidirectional helper methods
    public void addSeasonTeam(SeasonTeam seasonTeam) {
        boolean alreadyEnrolled = seasonTeams.stream()
            .anyMatch(st -> st.getTeam().equals(seasonTeam.getTeam()));
        if (alreadyEnrolled) {
            throw new DuplicateTeamException(
                seasonTeam.getTeam().getShortCode().value(), "enrolled in this season");
        }
        seasonTeams.add(seasonTeam);
        seasonTeam.setSeason(this);
    }

    public void removeSeasonTeam(SeasonTeam seasonTeam) {
        seasonTeams.remove(seasonTeam);
        seasonTeam.setSeason(null);
    }

    public void addRound(Round round) {
        rounds.add(round);
        round.setSeason(this);
    }

    public void removeRound(Round round) {
        rounds.remove(round);
        round.setSeason(null);
    }

    void setLeague(League league) {
        this.league = league;
    }

    // Domain Queries
    public boolean isActive() { return this.status == SeasonStatus.ACTIVE; }

    public int teamCount() { return seasonTeams.size(); }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public League getLeague() {
        return league;
    }

    public String getName() {
        return name;
    }

    public int getYearStart() {
        return yearStart;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public SeasonStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SeasonTeam> getSeasonTeams() {
        return Collections.unmodifiableList(seasonTeams);
    }

    public List<Round> getRounds() {
        return Collections.unmodifiableList(rounds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Season other)) return false;
        return Objects.equals(name, other.name) &&
                Objects.equals(league != null ? league.getSlug() : null,
                               other.league != null ? other.league.getSlug() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(league != null ? league.getSlug() : null, name);
    }

    @Override
    public String toString() {
        return "Season{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", yearStart=" + yearStart +
                ", yearEnd=" + yearEnd +
                ", status=" + status +
                '}';
    }
}
