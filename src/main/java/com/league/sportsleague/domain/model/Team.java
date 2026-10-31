package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.valueobject.TeamCode;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @Column(updatable = false)
    private UUID id;

    private String name;

    @Column(unique = true, updatable = false)
    private TeamCode shortCode;

    private String stadiumName;

    private String city;

    private Integer foundedYear;

    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SeasonTeam> seasonTeams = new ArrayList<>();

    public Team() {
        this.id = UUID.randomUUID();
    }

    public Team(String name, String shortCode, String stadiumName, String city, Integer foundedYear) {
        this();
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Team name must not be blank");
        Objects.requireNonNull(shortCode, "Short code must not be null");
        this.name = name;
        this.shortCode = new TeamCode(shortCode);
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

    // Domain Mutator
    public void updateProfile(String name, String stadiumName, String city, Integer foundedYear) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Team name must not be blank");
        this.name = name;
        this.stadiumName = stadiumName;
        this.city = city;
        this.foundedYear = foundedYear;
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TeamCode getShortCode() {
        return shortCode;
    }

    public String getStadiumName() {
        return stadiumName;
    }

    public String getCity() {
        return city;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<SeasonTeam> getSeasonTeams() {
        return Collections.unmodifiableList(seasonTeams);
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
