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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "teams")
public class Team {

    public static final String DEFAULT_SPORT = "FOOTBALL";

    @Id
    @Column(updatable = false)
    private UUID id;

    private String name;

    @Column(unique = true, updatable = false)
    private TeamCode shortCode;

    @Column(nullable = false, updatable = false)
    private String sportType = DEFAULT_SPORT;

    private String stadiumName;

    private String city;

    private Integer foundedYear;

    @Column(updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SeasonTeam> seasonTeams = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Player> players = new ArrayList<>();

    public Team() {
        this.id = UUID.randomUUID();
    }

    public Team(String name, String shortCode, String stadiumName, String city, Integer foundedYear) {
        this(name, shortCode, null, stadiumName, city, foundedYear);
    }

    public Team(String name, String shortCode, String sportType, String stadiumName, String city, Integer foundedYear) {
        this();
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Team name must not be blank");
        Objects.requireNonNull(shortCode, "Short code must not be null");
        this.name = name;
        this.shortCode = new TeamCode(shortCode);
        this.sportType = (sportType == null || sportType.isBlank())
                ? DEFAULT_SPORT : sportType.trim().toUpperCase();
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

    // Squad

    /** Adds a player to the squad. Shirt numbers are unique among the active players of the team. */
    public Player addPlayer(String name, int shirtNumber, String playingPosition) {
        requireFreeShirtNumber(shirtNumber, null);
        Player player = new Player(this, name, shirtNumber, playingPosition);
        players.add(player);
        return player;
    }

    public void updatePlayer(Player player, String name, int shirtNumber, String playingPosition) {
        requireOwnPlayer(player);
        requireFreeShirtNumber(shirtNumber, player);
        player.update(name, shirtNumber, playingPosition);
    }

    /** Removes a player from the squad. The player is deactivated, not deleted: match history keeps it. */
    public void removePlayer(Player player) {
        requireOwnPlayer(player);
        player.deactivate();
    }

    // Compared by id: within a session the same team or player can be reached as a Hibernate proxy.
    private void requireOwnPlayer(Player player) {
        Objects.requireNonNull(player, "Player must not be null");
        if (!this.id.equals(player.getTeam().getId())) {
            throw new IllegalArgumentException("Player does not belong to this team");
        }
    }

    private void requireFreeShirtNumber(int shirtNumber, Player except) {
        for (Player other : players) {
            boolean samePlayer = except != null && other.getId().equals(except.getId());
            if (!samePlayer && other.isActive() && other.getShirtNumber() == shirtNumber) {
                throw new IllegalArgumentException(
                        "Shirt number %d is already taken by %s".formatted(shirtNumber, other.getName()));
            }
        }
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

    public String getSportType() {
        return sportType;
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

    /** Every player ever registered, including removed ones. */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /** The current squad, ordered by shirt number. */
    public List<Player> getActivePlayers() {
        return players.stream()
                .filter(Player::isActive)
                .sorted(Comparator.comparingInt(Player::getShirtNumber))
                .toList();
    }

    /** Finds a player of this team by id, whether still active or removed. */
    public Optional<Player> findPlayer(UUID playerId) {
        return players.stream().filter(p -> p.getId().equals(playerId)).findFirst();
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
