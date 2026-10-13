package com.league.sportsleague.domain;

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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "match_events")
public class MatchEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private MatchEventType eventType;

    @Column(name = "minute", nullable = false)
    private int minute;

    @Column(name = "player_name", nullable = false, length = 100)
    private String playerName;

    // Optional attribute / relation
    @Column(name = "assisting_player", length = 100)
    private String assistingPlayer;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MatchEvent() {
        this.id = UUID.randomUUID();
    }

    public MatchEvent(Fixture fixture, Team team, MatchEventType eventType, int minute, String playerName) {
        this();
        this.fixture = fixture;
        this.team = team;
        this.eventType = eventType;
        this.minute = minute;
        this.playerName = playerName;
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

    public Fixture getFixture() {
        return fixture;
    }

    public void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public MatchEventType getEventType() {
        return eventType;
    }

    public void setEventType(MatchEventType eventType) {
        this.eventType = eventType;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getAssistingPlayer() {
        return assistingPlayer;
    }

    public void setAssistingPlayer(String assistingPlayer) {
        this.assistingPlayer = assistingPlayer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchEvent other)) return false;
        return minute == other.minute &&
                eventType == other.eventType &&
                Objects.equals(fixture != null ? fixture.getId() : null,
                               other.fixture != null ? other.fixture.getId() : null) &&
                Objects.equals(team != null ? team.getId() : null,
                               other.team != null ? other.team.getId() : null) &&
                Objects.equals(playerName, other.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixture != null ? fixture.getId() : null,
                            team != null ? team.getId() : null,
                            eventType, minute, playerName);
    }

    @Override
    public String toString() {
        return "MatchEvent{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", minute=" + minute +
                ", playerName='" + playerName + '\'' +
                '}';
    }
}
