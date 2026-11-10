package com.league.sportsleague.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Team squad")
class TeamSquadTest {

    private Team team;

    @BeforeEach
    void setUp() {
        team = new Team("GNK Dinamo Zagreb", "DIN", "Maksimir", "Zagreb", 1911);
    }

    @Nested
    @DisplayName("Shirt numbers")
    class ShirtNumbers {

        @Test
        @DisplayName("a number used by an active player cannot be given to another player")
        void duplicateActiveNumberRejected() {
            team.addPlayer("Bruno Petkovic", 9, "Forward");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> team.addPlayer("Sandro Kulenovic", 9, "Forward"));

            assertEquals("Shirt number 9 is already taken by Bruno Petkovic", ex.getMessage());
            assertEquals(1, team.getPlayers().size());
        }

        @Test
        @DisplayName("a number becomes free again once its player is removed")
        void numberReusableAfterRemoval() {
            Player old = team.addPlayer("Bruno Petkovic", 9, "Forward");
            team.removePlayer(old);

            Player successor = team.addPlayer("Sandro Kulenovic", 9, "Forward");

            assertEquals(List.of(successor), team.getActivePlayers());
            assertEquals(2, team.getPlayers().size());
        }

        @Test
        @DisplayName("updating a player keeps its own number without a conflict")
        void updateKeepingOwnNumberAllowed() {
            Player player = team.addPlayer("Bruno Petkovic", 9, "Forward");

            team.updatePlayer(player, "Bruno Petković", 9, "Striker");

            assertEquals("Bruno Petković", player.getName());
            assertEquals(9, player.getShirtNumber());
            assertEquals("Striker", player.getPlayingPosition());
        }

        @Test
        @DisplayName("updating a player to the number of a team-mate is rejected and changes nothing")
        void updateToTakenNumberRejected() {
            team.addPlayer("Bruno Petkovic", 9, "Forward");
            Player other = team.addPlayer("Martin Baturina", 10, "Midfielder");

            assertThrows(IllegalArgumentException.class,
                    () -> team.updatePlayer(other, "Martin Baturina", 9, "Midfielder"));

            assertEquals(10, other.getShirtNumber());
        }

        @Test
        @DisplayName("numbers outside 1..99 are rejected")
        void numberOutOfRangeRejected() {
            assertThrows(IllegalArgumentException.class, () -> team.addPlayer("Player", 0, null));
            assertThrows(IllegalArgumentException.class, () -> team.addPlayer("Player", 100, null));
            assertTrue(team.getPlayers().isEmpty());
        }
    }

    @Nested
    @DisplayName("Player data")
    class PlayerData {

        @Test
        @DisplayName("a blank name is rejected")
        void blankNameRejected() {
            assertThrows(IllegalArgumentException.class, () -> team.addPlayer("  ", 7, null));
            assertTrue(team.getPlayers().isEmpty());
        }

        @Test
        @DisplayName("the name is trimmed and a blank position is stored as null")
        void nameTrimmedAndBlankPositionNull() {
            Player player = team.addPlayer("  Josip Misic ", 27, "   ");

            assertEquals("Josip Misic", player.getName());
            assertNull(player.getPlayingPosition());
        }
    }

    @Nested
    @DisplayName("Squad membership")
    class Membership {

        @Test
        @DisplayName("the active squad excludes removed players and is ordered by shirt number")
        void activePlayersExcludeRemovedAndAreSorted() {
            Player ten = team.addPlayer("Martin Baturina", 10, "Midfielder");
            Player one = team.addPlayer("Dominik Livakovic", 1, "Goalkeeper");
            Player nine = team.addPlayer("Bruno Petkovic", 9, "Forward");
            team.removePlayer(nine);

            assertEquals(List.of(one, ten), team.getActivePlayers());
            assertFalse(nine.isActive());
        }

        @Test
        @DisplayName("a removed player is still found by id, so history can reference it")
        void removedPlayerStillFoundById() {
            Player nine = team.addPlayer("Bruno Petkovic", 9, "Forward");
            team.removePlayer(nine);

            assertEquals(nine, team.findPlayer(nine.getId()).orElseThrow());
        }

        @Test
        @DisplayName("a player of another team cannot be updated or removed through this team")
        void foreignPlayerRejected() {
            Team other = new Team("HNK Hajduk Split", "HAJ", "Poljud", "Split", 1911);
            Player foreign = other.addPlayer("Marko Livaja", 10, "Forward");

            assertThrows(IllegalArgumentException.class, () -> team.removePlayer(foreign));
            assertThrows(IllegalArgumentException.class,
                    () -> team.updatePlayer(foreign, "Marko Livaja", 11, "Forward"));
            assertTrue(foreign.isActive());
            assertEquals(10, foreign.getShirtNumber());
        }
    }
}
