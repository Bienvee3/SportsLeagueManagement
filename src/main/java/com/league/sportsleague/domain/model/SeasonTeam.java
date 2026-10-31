package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.model.enums.SeasonTeamStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "season_teams",
    uniqueConstraints = @UniqueConstraint(name = "uk_season_teams_season_team", columnNames = {"season_id", "team_id"})
)
public class SeasonTeam {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    private int initialSeed = 0;

    @Column(updatable = false)
    private Instant registrationDate;

    @Enumerated(EnumType.STRING)
    private SeasonTeamStatus status = SeasonTeamStatus.CONFIRMED;

    public SeasonTeam() {
        this.id = UUID.randomUUID();
    }

    public SeasonTeam(Season season, Team team, int initialSeed, SeasonTeamStatus status) {
        this();
        this.season = season;
        this.team = team;
        this.initialSeed = initialSeed;
        this.status = (status != null) ? status : SeasonTeamStatus.CONFIRMED;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.registrationDate == null) {
            this.registrationDate = Instant.now();
        }
    }

    // Domain Mutators
    public void updateSeed(int seed) {
        this.initialSeed = seed;
    }

    public void updateStatus(SeasonTeamStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public void withdraw() {
        this.status = SeasonTeamStatus.WITHDRAWN;
    }

    public void disqualify() {
        this.status = SeasonTeamStatus.DISQUALIFIED;
    }

    void setSeason(Season season) {
        this.season = season;
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public Season getSeason() {
        return season;
    }

    public Team getTeam() {
        return team;
    }

    public int getInitialSeed() {
        return initialSeed;
    }

    public Instant getRegistrationDate() {
        return registrationDate;
    }

    public SeasonTeamStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeasonTeam other)) return false;
        return Objects.equals(season != null ? season.getId() : null,
                              other.season != null ? other.season.getId() : null) &&
               Objects.equals(team != null ? team.getId() : null,
                              other.team != null ? other.team.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(season != null ? season.getId() : null,
                            team != null ? team.getId() : null);
    }

    @Override
    public String toString() {
        return "SeasonTeam{" +
                "id=" + id +
                ", initialSeed=" + initialSeed +
                ", status=" + status +
                '}';
    }
}
