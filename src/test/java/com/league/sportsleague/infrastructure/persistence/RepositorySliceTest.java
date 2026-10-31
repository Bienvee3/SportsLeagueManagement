package com.league.sportsleague.infrastructure.persistence;

import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.repository.*;
import com.league.sportsleague.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Data JPA slice tests for all repository contracts.
 * Uses H2 in PostgreSQL compatibility mode (test profile).
 * Flyway V1+V2 migrations run before tests → seed data is available.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositorySliceTest {

    // Known UUIDs from V2__seed_initial_data.sql
    private static final UUID LEAGUE_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEASON_ID  = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ROUND_ID   = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID FIXTURE1_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID FIXTURE2_ID = UUID.fromString("66666666-6666-6666-6666-666666666602");
    private static final UUID DINAMO_ID  = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID HAJDUK_ID  = UUID.fromString("22222222-2222-2222-2222-222222222202");

    @Autowired LeagueRepository leagueRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired SeasonRepository seasonRepository;
    @Autowired RoundRepository roundRepository;
    @Autowired FixtureRepository fixtureRepository;

    @Autowired TestEntityManager em;

    // ── LeagueRepository ────────────────────────────────────────────────────

    @Nested
    @DisplayName("LeagueRepository")
    class LeagueRepoTests {

        @Test
        @DisplayName("findById returns seeded SuperSport HNL league")
        void findByIdReturnsSeedLeague() {
            Optional<League> result = leagueRepository.findById(LEAGUE_ID);
            assertTrue(result.isPresent());
            assertEquals("SuperSport HNL", result.get().getName());
            assertEquals("supersport-hnl", result.get().getSlug().value());
        }

        @Test
        @DisplayName("findBySlug with Slug VO returns correct league")
        void findBySlugReturnsLeague() {
            Optional<League> result = leagueRepository.findBySlug(new Slug("supersport-hnl"));
            assertTrue(result.isPresent());
            assertEquals(LEAGUE_ID, result.get().getId());
        }

        @Test
        @DisplayName("findBySlug with unknown slug returns empty")
        void findBySlugUnknownReturnsEmpty() {
            Optional<League> result = leagueRepository.findBySlug(new Slug("unknown-league"));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findAll returns at least the seeded league")
        void findAllContainsSeedLeague() {
            List<League> all = leagueRepository.findAll();
            assertFalse(all.isEmpty());
            assertTrue(all.stream().anyMatch(l -> l.getId().equals(LEAGUE_ID)));
        }

        @Test
        @DisplayName("save persists a new league and findById retrieves it")
        void saveAndFindById() {
            League newLeague = new League("Prva HNL Test", "prva-hnl-test", "FOOTBALL", "Croatia");
            League saved = leagueRepository.save(newLeague);

            em.flush();
            em.clear();

            Optional<League> found = leagueRepository.findById(saved.getId());
            assertTrue(found.isPresent());
            assertEquals("prva-hnl-test", found.get().getSlug().value());
        }

        @Test
        @DisplayName("Slug converter round-trip: save Slug → load → Slug.value() matches")
        void slugConverterRoundTrip() {
            League league = new League("Test Liga", "test-liga-2026", "FOOTBALL", "Croatia");
            leagueRepository.save(league);
            em.flush();
            em.clear();

            Optional<League> found = leagueRepository.findBySlug(new Slug("test-liga-2026"));
            assertTrue(found.isPresent());
            assertEquals("test-liga-2026", found.get().getSlug().value());
            assertInstanceOf(Slug.class, found.get().getSlug());
        }
    }

    // ── TeamRepository ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("TeamRepository")
    class TeamRepoTests {

        @Test
        @DisplayName("findById returns seeded Dinamo Zagreb")
        void findByIdReturnsSeedTeam() {
            Optional<Team> result = teamRepository.findById(DINAMO_ID);
            assertTrue(result.isPresent());
            assertEquals("GNK Dinamo Zagreb", result.get().getName());
        }

        @Test
        @DisplayName("findByShortCode with TeamCode VO returns correct team")
        void findByShortCodeReturnsTeam() {
            Optional<Team> result = teamRepository.findByShortCode(new TeamCode("DIN"));
            assertTrue(result.isPresent());
            assertEquals(DINAMO_ID, result.get().getId());
            assertEquals("DIN", result.get().getShortCode().value());
        }

        @Test
        @DisplayName("findByShortCode with unknown code returns empty")
        void findByShortCodeUnknownReturnsEmpty() {
            Optional<Team> result = teamRepository.findByShortCode(new TeamCode("ZZZ"));
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findAll returns the 4 seeded teams")
        void findAllReturnsFourTeams() {
            List<Team> teams = teamRepository.findAll();
            assertTrue(teams.size() >= 4);
        }

        @Test
        @DisplayName("TeamCode converter round-trip: save → load → getShortCode() is TeamCode")
        void teamCodeConverterRoundTrip() {
            Team team = new Team("NK Test", "TST", "Test Stadium", "Test City", 2000);
            teamRepository.save(team);
            em.flush();
            em.clear();

            Optional<Team> found = teamRepository.findByShortCode(new TeamCode("TST"));
            assertTrue(found.isPresent());
            assertInstanceOf(TeamCode.class, found.get().getShortCode());
            assertEquals("TST", found.get().getShortCode().value());
        }
    }

    // ── SeasonRepository ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("SeasonRepository")
    class SeasonRepoTests {

        @Test
        @DisplayName("findById returns seeded 2026/2027 season")
        void findByIdReturnsSeedSeason() {
            Optional<Season> result = seasonRepository.findById(SEASON_ID);
            assertTrue(result.isPresent());
            assertEquals("2026/2027 Season", result.get().getName());
            assertEquals(SeasonStatus.ACTIVE, result.get().getStatus());
        }

        @Test
        @DisplayName("findByLeagueId returns seasons for the HNL league")
        void findByLeagueIdReturnsSeason() {
            List<Season> seasons = seasonRepository.findByLeagueId(LEAGUE_ID);
            assertFalse(seasons.isEmpty());
            assertEquals(SEASON_ID, seasons.get(0).getId());
        }

        @Test
        @DisplayName("findByLeagueId for unknown league returns empty list")
        void findByLeagueIdUnknownReturnsEmpty() {
            List<Season> seasons = seasonRepository.findByLeagueId(UUID.randomUUID());
            assertTrue(seasons.isEmpty());
        }
    }

    // ── RoundRepository ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("RoundRepository")
    class RoundRepoTests {

        @Test
        @DisplayName("findById returns seeded Round 1")
        void findByIdReturnsSeedRound() {
            Optional<Round> result = roundRepository.findById(ROUND_ID);
            assertTrue(result.isPresent());
            assertEquals(1, result.get().getRoundNumber());
            assertEquals("Round 1", result.get().getName());
        }

        @Test
        @DisplayName("findBySeasonIdOrderByRoundNumber returns rounds in order")
        void findBySeasonIdOrderByRoundNumber() {
            List<Round> rounds = roundRepository.findBySeasonIdOrderByRoundNumber(SEASON_ID);
            assertFalse(rounds.isEmpty());
            assertEquals(ROUND_ID, rounds.get(0).getId());
            assertEquals(1, rounds.get(0).getRoundNumber());
        }

        @Test
        @DisplayName("findBySeasonIdOrderByRoundNumber for unknown season returns empty")
        void findBySeasonIdUnknownReturnsEmpty() {
            List<Round> rounds = roundRepository.findBySeasonIdOrderByRoundNumber(UUID.randomUUID());
            assertTrue(rounds.isEmpty());
        }
    }

    // ── FixtureRepository ────────────────────────────────────────────────────

    @Nested
    @DisplayName("FixtureRepository")
    class FixtureRepoTests {

        @Test
        @DisplayName("findById returns seeded Dinamo vs Hajduk fixture")
        void findByIdReturnsSeedFixture() {
            Optional<Fixture> result = fixtureRepository.findById(FIXTURE1_ID);
            assertTrue(result.isPresent());
            assertEquals(FixtureStatus.FINISHED, result.get().getStatus());
            assertEquals(2, result.get().getHomeScore());
            assertEquals(1, result.get().getAwayScore());
        }

        @Test
        @DisplayName("findByRoundId returns the 2 fixtures in Round 1")
        void findByRoundIdReturnsTwoFixtures() {
            List<Fixture> fixtures = fixtureRepository.findByRoundId(ROUND_ID);
            assertEquals(2, fixtures.size());
            assertTrue(fixtures.stream().anyMatch(f -> f.getId().equals(FIXTURE1_ID)));
            assertTrue(fixtures.stream().anyMatch(f -> f.getId().equals(FIXTURE2_ID)));
        }

        @Test
        @DisplayName("findFinishedBySeasonWithTeams returns 2 FINISHED fixtures with teams loaded")
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        void findFinishedBySeasonWithTeamsLoadsTeamsEagerly() {
            List<Fixture> fixtures = fixtureRepository.findFinishedBySeasonWithTeams(SEASON_ID);

            assertEquals(2, fixtures.size());
            // Access team properties OUTSIDE a transaction to prove JOIN FETCH worked
            // (no LazyInitializationException expected)
            for (Fixture f : fixtures) {
                assertNotNull(f.getHomeTeam().getName(),
                    "homeTeam must be eagerly loaded via JOIN FETCH");
                assertNotNull(f.getAwayTeam().getName(),
                    "awayTeam must be eagerly loaded via JOIN FETCH");
                assertEquals(FixtureStatus.FINISHED, f.getStatus());
            }
        }

        @Test
        @DisplayName("findByRoundId for unknown round returns empty")
        void findByRoundIdUnknownReturnsEmpty() {
            List<Fixture> fixtures = fixtureRepository.findByRoundId(UUID.randomUUID());
            assertTrue(fixtures.isEmpty());
        }
    }
}
