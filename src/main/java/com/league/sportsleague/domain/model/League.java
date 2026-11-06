package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.valueobject.Slug;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "leagues")
public class League {

    @Id
    @Column(updatable = false)
    private UUID id;

    private String name;

    @Column(unique = true, updatable = false)
    private Slug slug;

    private String sportType = "FOOTBALL";

    private String country;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Season> seasons = new ArrayList<>();

    public League() {
        this.id = UUID.randomUUID();
    }

    public League(String name, String slug, String sportType, String country) {
        this();
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("League name must not be blank");
        Objects.requireNonNull(country, "Country must not be null");
        if (country.isBlank())
            throw new IllegalArgumentException("Country must not be blank");
        this.name = name;
        this.slug = new Slug(slug);
        this.sportType = (sportType != null && !sportType.isBlank()) ? sportType : "FOOTBALL";
        this.country = country;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Domain Mutators
    public void rename(String name, String country, String sportType) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("League name must not be blank");
        this.name = name;
        this.country = country;
        if (sportType != null && !sportType.isBlank()) {
            this.sportType = sportType;
        }
    }

    // Bidirectional Collection Synchronization
    public void addSeason(Season season) {
        seasons.add(season);
        season.setLeague(this);
    }

    public void removeSeason(Season season) {
        seasons.remove(season);
        season.setLeague(null);
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Slug getSlug() {
        return slug;
    }

    public String getSportType() {
        return sportType;
    }

    public String getCountry() {
        return country;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Season> getSeasons() {
        return Collections.unmodifiableList(seasons);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof League other)) return false;
        return Objects.equals(slug, other.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }

    @Override
    public String toString() {
        return "League{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", sportType='" + sportType + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}
