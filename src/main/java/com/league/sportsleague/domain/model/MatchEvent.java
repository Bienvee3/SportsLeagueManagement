package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.valueobject.MatchMinute;
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

import com.league.sportsleague.domain.model.enums.MatchEventType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "match_events")
public class MatchEvent {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    private MatchEventType eventType;

    private MatchMinute matchMinute;

    private String playerName;

    // Optional attribute required by LO4
    private String assistingPlayer;

    /** Squad player the event belongs to; null for a legacy event recorded with a free-text name. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    /** Assist provider of a GOAL/PENALTY, or the player coming on in a SUBSTITUTION. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_player_id")
    private Player relatedPlayer;

    private String notes;

    @Column(updatable = false)
    private Instant createdAt;

    public MatchEvent() {
        this.id = UUID.randomUUID();
    }

    public MatchEvent(Fixture fixture, Team team, MatchEventType eventType, int matchMinute, String playerName) {
        this(fixture, team, eventType, matchMinute, playerName, null, null);
    }

    public MatchEvent(Fixture fixture, Team team, MatchEventType eventType, int matchMinute,
                      String playerName, String assistingPlayer, String notes) {
        this();
        this.fixture = fixture;
        this.team = team;
        this.eventType = eventType;
        this.matchMinute = new MatchMinute(matchMinute);
        this.playerName = playerName;
        this.assistingPlayer = assistingPlayer;
        this.notes = notes;
    }

    /**
     * Event of a squad player. The name columns keep a snapshot of the names, so the event stays
     * readable wherever only the text is used (chronicle, timeline).
     *
     * @param player        the scorer, the booked player, or the player going off in a SUBSTITUTION
     * @param relatedPlayer assist provider (GOAL/PENALTY) or player coming on (SUBSTITUTION); may be null
     */
    public MatchEvent(Fixture fixture, Team team, MatchEventType eventType, int matchMinute,
                      Player player, Player relatedPlayer, String notes) {
        this(fixture, team, eventType, matchMinute,
                Objects.requireNonNull(player, "Player must not be null").getName(),
                relatedPlayer == null ? null : relatedPlayer.getName(),
                notes);
        this.player = player;
        this.relatedPlayer = relatedPlayer;
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

    void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public Team getTeam() {
        return team;
    }

    public MatchEventType getEventType() {
        return eventType;
    }

    public MatchMinute getMatchMinute() {
        return matchMinute;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAssistingPlayer() {
        return assistingPlayer;
    }

    public Player getPlayer() {
        return player;
    }

    public Player getRelatedPlayer() {
        return relatedPlayer;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchEvent other)) return false;
        return Objects.equals(matchMinute, other.matchMinute) &&
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
                            eventType, matchMinute, playerName);
    }

    @Override
    public String toString() {
        String minute = null;
        return "MatchEvent{" +
                "id=" + id +
                ", eventType=" + eventType +
                ", minute=" + minute +
                ", playerName='" + playerName + '\'' +
                '}';
    }
}
