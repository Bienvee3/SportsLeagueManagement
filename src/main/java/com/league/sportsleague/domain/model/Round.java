package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.model.enums.RoundStatus;
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
import java.util.Collections;
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
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    private int roundNumber;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private RoundStatus status = RoundStatus.SCHEDULED;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Fixture> fixtures = new ArrayList<>();

    public Round() {
        this.id = UUID.randomUUID();
    }

    public Round(Season season, int roundNumber, String name, LocalDate startDate, LocalDate endDate, RoundStatus status) {
        this();
        Objects.requireNonNull(season, "Season must not be null");
        if (roundNumber < 1)
            throw new IllegalArgumentException("Round number must be >= 1, got: " + roundNumber);
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

    // Domain Mutators
    public void startRound() {
        if (this.status != RoundStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Round", status.name(), "startRound");
        }
        this.status = RoundStatus.IN_PROGRESS;
    }

    public void completeRound() {
        if (this.status != RoundStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("Round", status.name(), "completeRound");
        }
        this.status = RoundStatus.COMPLETED;
    }

    /**
     * Keeps the round in step with its matches (used by the scheduled lifecycle job).
     * SCHEDULED becomes IN_PROGRESS once any match has kicked off or finished; IN_PROGRESS becomes
     * COMPLETED once the round has matches and every one is FINISHED or CANCELLED — a POSTPONED or
     * still SCHEDULED match keeps it open. A SCHEDULED round whose matches were all recorded at once
     * therefore completes in a single call.
     *
     * @return true when the status changed
     */
    public boolean advanceFromFixtures() {
        RoundStatus before = this.status;
        if (this.status == RoundStatus.SCHEDULED && fixtures.stream().anyMatch(Round::hasStarted)) {
            this.status = RoundStatus.IN_PROGRESS;
        }
        if (this.status == RoundStatus.IN_PROGRESS && !fixtures.isEmpty()
                && fixtures.stream().allMatch(Round::isSettled)) {
            this.status = RoundStatus.COMPLETED;
        }
        return this.status != before;
    }

    private static boolean hasStarted(Fixture f) {
        return f.getStatus() == FixtureStatus.IN_PROGRESS || f.isFinished();
    }

    private static boolean isSettled(Fixture f) {
        return f.isFinished() || f.getStatus() == FixtureStatus.CANCELLED;
    }

    public void updateSchedule(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** Changes the round number; a default "Round N" name follows the number, a custom name is kept. */
    public void renumber(int newNumber) {
        if (newNumber < 1)
            throw new IllegalArgumentException("Round number must be >= 1, got: " + newNumber);
        if (("Round " + this.roundNumber).equals(this.name)) {
            this.name = "Round " + newNumber;
        }
        this.roundNumber = newNumber;
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

    void setSeason(Season season) {
        this.season = season;
    }

    // Domain Queries
    public boolean allFixturesFinished() {
        return !fixtures.isEmpty() && fixtures.stream().allMatch(Fixture::isFinished);
    }

    // Read-only Accessors
    public UUID getId() {
        return id;
    }

    public Season getSeason() {
        return season;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public List<Fixture> getFixtures() {
        return Collections.unmodifiableList(fixtures);
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
