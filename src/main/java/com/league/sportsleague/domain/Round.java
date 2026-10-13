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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "rounds",
    uniqueConstraints = @UniqueConstraint(name = "uk_rounds_season_round_number", columnNames = {"season_id", "round_number"})
)
public class Round {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoundStatus status = RoundStatus.SCHEDULED;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Fixture> fixtures = new ArrayList<>();

    public Round() {
        this.id = UUID.randomUUID();
    }

    public Round(Season season, int roundNumber, String name, LocalDate startDate, LocalDate endDate, RoundStatus status) {
        this();
        this.season = season;
        this.roundNumber = roundNumber;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = (status != null) ? status : RoundStatus.SCHEDULED;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    // Bidirectional helper methods
    public void addFixture(Fixture fixture) {
        fixtures.add(fixture);
        fixture.setRound(this);
    }

    public void removeFixture(Fixture fixture) {
        fixtures.remove(fixture);
        fixture.setRound(null);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public List<Fixture> getFixtures() {
        return fixtures;
    }

    public void setFixtures(List<Fixture> fixtures) {
        this.fixtures = fixtures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Round other)) return false;
        return roundNumber == other.roundNumber &&
                Objects.equals(season != null ? season.getId() : null,
                               other.season != null ? other.season.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(season != null ? season.getId() : null, roundNumber);
    }

    @Override
    public String toString() {
        return "Round{" +
                "id=" + id +
                ", roundNumber=" + roundNumber +
                ", name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
