package com.league.sportsleague.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A squad member of one team. Players are created, changed and removed through {@link Team}, which
 * guards the shirt numbers. A removed player is only deactivated so lineups and match events keep
 * their reference.
 */
@Entity
@Table(name = "players")
public class Player {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, updatable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int shirtNumber;

    private String playingPosition;

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private Instant createdAt;

    protected Player() {
        this.id = UUID.randomUUID();
    }

    Player(Team team, String name, int shirtNumber, String playingPosition) {
        this();
        this.team = Objects.requireNonNull(team, "Team must not be null");
        update(name, shirtNumber, playingPosition);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    void update(String name, int shirtNumber, String playingPosition) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Player name must not be blank");
        if (shirtNumber < 1 || shirtNumber > 99)
            throw new IllegalArgumentException("Shirt number must be between 1 and 99, got: " + shirtNumber);
        this.name = name.trim();
        this.shirtNumber = shirtNumber;
        this.playingPosition = (playingPosition == null || playingPosition.isBlank())
                ? null : playingPosition.trim();
    }

    void deactivate() {
        this.active = false;
    }

    public UUID getId() { return id; }
    public Team getTeam() { return team; }
    public String getName() { return name; }
    public int getShirtNumber() { return shirtNumber; }
    public String getPlayingPosition() { return playingPosition; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player other)) return false;
        return Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{id=" + id + ", name='" + name + "', shirtNumber=" + shirtNumber + '}';
    }
}
