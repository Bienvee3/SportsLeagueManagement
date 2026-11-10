package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.model.enums.LineupRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * One player named on the match sheet of a fixture, as a starter or as a substitute. Entries are
 * owned by {@link Fixture} and only change through {@link Fixture#submitLineup} and
 * {@link Fixture#clearLineup}.
 */
@Entity
@Table(name = "lineup_entries")
public class LineupEntry {

    @Id
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false, updatable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @Column(name = "lineup_role", nullable = false)
    private LineupRole lineupRole;

    @Column(nullable = false)
    private boolean captain;

    protected LineupEntry() {
        this.id = UUID.randomUUID();
    }

    LineupEntry(Fixture fixture, Player player, LineupRole lineupRole, boolean captain) {
        this();
        this.fixture = Objects.requireNonNull(fixture, "Fixture must not be null");
        this.player = Objects.requireNonNull(player, "Player must not be null");
        reassign(lineupRole, captain);
    }

    void reassign(LineupRole lineupRole, boolean captain) {
        this.lineupRole = Objects.requireNonNull(lineupRole, "Lineup role must not be null");
        this.captain = captain;
    }

    void setFixture(Fixture fixture) {
        this.fixture = fixture;
    }

    public UUID getId() { return id; }
    public Fixture getFixture() { return fixture; }
    public Player getPlayer() { return player; }
    public LineupRole getLineupRole() { return lineupRole; }
    public boolean isCaptain() { return captain; }

    /** The team this entry belongs to: the team of its player. */
    public Team getTeam() { return player.getTeam(); }

    public boolean isStarter() { return lineupRole == LineupRole.STARTER; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineupEntry other)) return false;
        return Objects.equals(fixture != null ? fixture.getId() : null,
                              other.getFixture() != null ? other.getFixture().getId() : null) &&
               Objects.equals(player != null ? player.getId() : null,
                              other.getPlayer() != null ? other.getPlayer().getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixture != null ? fixture.getId() : null,
                            player != null ? player.getId() : null);
    }
}
