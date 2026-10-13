package com.league.sportsleague.domain;

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

@Entity
@Table(name = "result_audits")
public class ResultAudit {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Column(name = "old_home_score")
    private Integer oldHomeScore;

    @Column(name = "old_away_score")
    private Integer oldAwayScore;

    @Column(name = "new_home_score", nullable = false)
    private int newHomeScore;

    @Column(name = "new_away_score", nullable = false)
    private int newAwayScore;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "corrected_by", nullable = false, length = 100)
    private String correctedBy;

    @Column(name = "corrected_at", nullable = false, updatable = false)
    private Instant correctedAt;

    public ResultAudit() {
        this.id = UUID.randomUUID();
    }

    public ResultAudit(Fixture fixture, Integer oldHomeScore, Integer oldAwayScore,
                       int newHomeScore, int newAwayScore, String reason, String correctedBy) {
        this();
        this.fixture = fixture;
        this.oldHomeScore = oldHomeScore;
        this.oldAwayScore = oldAwayScore;
        this.newHomeScore = newHomeScore;
        this.newAwayScore = newAwayScore;
        this.reason = reason;
        this.correctedBy = correctedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.correctedAt == null) {
            this.correctedAt = Instant.now();
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

    public Integer getOldHomeScore() {
        return oldHomeScore;
    }

    public void setOldHomeScore(Integer oldHomeScore) {
        this.oldHomeScore = oldHomeScore;
    }

    public Integer getOldAwayScore() {
        return oldAwayScore;
    }

    public void setOldAwayScore(Integer oldAwayScore) {
        this.oldAwayScore = oldAwayScore;
    }

    public int getNewHomeScore() {
        return newHomeScore;
    }

    public void setNewHomeScore(int newHomeScore) {
        this.newHomeScore = newHomeScore;
    }

    public int getNewAwayScore() {
        return newAwayScore;
    }

    public void setNewAwayScore(int newAwayScore) {
        this.newAwayScore = newAwayScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public void setCorrectedBy(String correctedBy) {
        this.correctedBy = correctedBy;
    }

    public Instant getCorrectedAt() {
        return correctedAt;
    }

    public void setCorrectedAt(Instant correctedAt) {
        this.correctedAt = correctedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResultAudit other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ResultAudit{" +
                "id=" + id +
                ", oldHomeScore=" + oldHomeScore +
                ", oldAwayScore=" + oldAwayScore +
                ", newHomeScore=" + newHomeScore +
                ", newAwayScore=" + newAwayScore +
                ", correctedBy='" + correctedBy + '\'' +
                ", correctedAt=" + correctedAt +
                '}';
    }
}
