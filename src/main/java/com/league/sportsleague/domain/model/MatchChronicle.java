package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.ai.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An AI-written (or AI-translated) match chronicle, stored per fixture and language so a language
 * switch can reuse or translate it instead of generating a new story.
 */
@Entity
@Table(name = "match_chronicles")
public class MatchChronicle {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID fixtureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(updatable = false)
    private Instant createdAt;

    protected MatchChronicle() {
        this.id = UUID.randomUUID();
    }

    public MatchChronicle(UUID fixtureId, Language language, String content) {
        this();
        this.fixtureId = Objects.requireNonNull(fixtureId, "Fixture id must not be null");
        this.language = Objects.requireNonNull(language, "Language must not be null");
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("Chronicle content must not be blank");
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getFixtureId() { return fixtureId; }
    public Language getLanguage() { return language; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchChronicle other)) return false;
        return Objects.equals(fixtureId, other.fixtureId) && language == other.language;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixtureId, language);
    }
}
