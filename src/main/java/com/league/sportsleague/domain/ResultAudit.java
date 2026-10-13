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
    @Column(updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Column(updatable = false)
    private Integer oldHomeScore;

    @Column(updatable = false)
    private Integer oldAwayScore;

    @Column(updatable = false)
    private int newHomeScore;

    @Column(updatable = false)
    private int newAwayScore;

    @Column(updatable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(updatable = false)
    private String correctedBy;

    @Column(updatable = false)
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

    public Integer getOldHomeScore() {
        return oldHomeScore;
    }

    public Integer getOldAwayScore() {
        return oldAwayScore;
    }

    public int getNewHomeScore() {
        return newHomeScore;
    }

    public int getNewAwayScore() {
        return newAwayScore;
    }

    public String getReason() {
        return reason;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public Instant getCorrectedAt() {
        return correctedAt;
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
