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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
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
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "year_start", nullable = false)
    private int yearStart;

    @Column(name = "year_end", nullable = false)
    private int yearEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeasonStatus status = SeasonStatus.UPCOMING;

    @Column(name = "created_at", nullable = false, updatable = false)
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

    // Bidirectional helper methods
    public void addSeasonTeam(SeasonTeam seasonTeam) {
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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public League getLeague() {
        return league;
    }

    public void setLeague(League league) {
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYearStart() {
        return yearStart;
    }

    public void setYearStart(int yearStart) {
        this.yearStart = yearStart;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    public SeasonStatus getStatus() {
        return status;
    }

    public void setStatus(SeasonStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<SeasonTeam> getSeasonTeams() {
        return seasonTeams;
    }

    public void setSeasonTeams(List<SeasonTeam> seasonTeams) {
        this.seasonTeams = seasonTeams;
    }

    public List<Round> getRounds() {
        return rounds;
    }

    public void setRounds(List<Round> rounds) {
        this.rounds = rounds;
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
