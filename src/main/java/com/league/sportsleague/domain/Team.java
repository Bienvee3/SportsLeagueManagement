package com.league.sportsleague.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "stadium_name", nullable = false, length = 100)
    private String stadiumName;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "founded_year")
    private Integer foundedYear;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SeasonTeam> seasonTeams = new ArrayList<>();

    public Team() {
        this.id = UUID.randomUUID();
    }

    public Team(String name, String shortCode, String stadiumName, String city, Integer foundedYear) {
        this();
        this.name = name;
        this.shortCode = shortCode;
        this.stadiumName = stadiumName;
        this.city = city;
        this.foundedYear = foundedYear;
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

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public void setStadiumName(String stadiumName) {
        this.stadiumName = stadiumName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team other)) return false;
        return Objects.equals(shortCode, other.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortCode);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", shortCode='" + shortCode + '\'' +
                ", stadiumName='" + stadiumName + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
