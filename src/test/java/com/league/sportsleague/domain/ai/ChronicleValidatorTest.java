package com.league.sportsleague.domain.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronicleValidatorTest {

    private static final String HOME = "Dinamo Zagreb";
    private static final String AWAY = "Hajduk Split";

    private static String filler(int words) {
        return "word ".repeat(words).trim();
    }

    private static ChronicleRequest request(int home, int away, List<MatchEventSummary> events) {
        return new ChronicleRequest(HOME, AWAY, home, away, events, Language.ENGLISH, List.of(), List.of());
    }

    private static MatchEventSummary event(int minute, String type, String team) {
        return new MatchEventSummary(minute, type, team, "Player");
    }

    /** A 160-word text stating the given score line. */
    private static String text(String scoreLine) {
        return "Dinamo Zagreb beat Hajduk Split " + scoreLine + " on a warm evening. " + filler(150);
    }

    @Nested
    @DisplayName("generated chronicle")
    class Generated {

        @Test
        @DisplayName("a text with the real final score and a sensible length is accepted")
        void validTextIsAccepted() {
            assertTrue(ChronicleValidator.problems(request(2, 1, List.of()), text("2-1")).isEmpty());
        }

        @Test
        @DisplayName("an empty answer is rejected")
        void blankIsRejected() {
            assertEquals(1, ChronicleValidator.problems(request(2, 1, List.of()), "  ").size());
        }

        @Test
        @DisplayName("the final score must be stated")
        void finalScoreMustAppear() {
            List<String> problems = ChronicleValidator.problems(request(2, 1, List.of()), text("with a late winner"));

            assertTrue(problems.stream().anyMatch(p -> p.contains("2-1")), problems.toString());
        }

        @Test
        @DisplayName("a scoreline that is not in the data is rejected as invented")
        void inventedScorelineIsRejected() {
            List<String> problems = ChronicleValidator.problems(request(2, 1, List.of()), text("3-1") + " Final 2-1.");

            assertTrue(problems.stream().anyMatch(p -> p.contains("3-1")), problems.toString());
        }

        @Test
        @DisplayName("running scores after each goal are allowed, in either dash style")
        void runningScoresAreAllowed() {
            List<MatchEventSummary> events = List.of(
                    event(10, "GOAL", HOME), event(55, "GOAL", AWAY), event(80, "PENALTY", HOME));

            String body = text("2–1") + " It was 1-0 early, 1:1 after the break.";

            assertTrue(ChronicleValidator.problems(request(2, 1, events), body).isEmpty());
        }

        @Test
        @DisplayName("an own goal counts for the opponent when the running score is derived")
        void ownGoalCountsForTheOpponent() {
            List<MatchEventSummary> events = List.of(event(30, "OWN_GOAL", HOME));

            assertTrue(ChronicleValidator.problems(request(0, 1, events), text("0-1") + " It was 0-1 at half time.").isEmpty());
            assertEquals(1, ChronicleValidator.problems(request(0, 1, events), text("0-1") + " It was 1-0 at half time.").size());
        }

        @Test
        @DisplayName("a clock time such as 18:00 is not mistaken for a scoreline")
        void clockTimesAreIgnored() {
            assertTrue(ChronicleValidator.problems(request(2, 1, List.of()), text("2-1") + " Kick-off was at 18:00.").isEmpty());
        }

        @Test
        @DisplayName("leaked drafting notes of the model are rejected")
        void leakedNotesAreRejected() {
            String leaked = text("2-1") + " Count is 146. Need 150 - 200 words.";

            assertTrue(ChronicleValidator.problems(request(2, 1, List.of()), leaked).stream()
                    .anyMatch(p -> p.toLowerCase().contains("notes")), "the leaked count must be reported");
        }

        @Test
        @DisplayName("running word counts in parentheses and markdown are rejected")
        void wordCountMarkersAreRejected() {
            assertEquals(1, ChronicleValidator.problems(request(2, 1, List.of()), text("2-1") + " (146) ").size());
            assertEquals(1, ChronicleValidator.problems(request(2, 1, List.of()), "**" + text("2-1")).size());
        }

        @Test
        @DisplayName("far too short or far too long answers are rejected")
        void lengthIsChecked() {
            assertTrue(ChronicleValidator.problems(request(2, 1, List.of()), "Dinamo won 2-1.").stream()
                    .anyMatch(p -> p.contains("words")));
            assertTrue(ChronicleValidator.problems(request(2, 1, List.of()), "Dinamo won 2-1. " + filler(400)).stream()
                    .anyMatch(p -> p.contains("words")));
        }

        @Test
        @DisplayName("a goalless draw only needs 0-0")
        void goallessDraw() {
            assertTrue(ChronicleValidator.problems(request(0, 0, List.of()), text("0-0")).isEmpty());
        }
    }

    @Nested
    @DisplayName("translation")
    class Translation {

        private static final String SOURCE = "Dinamo Zagreb won 2-1 after leading 1-0 at the break.";

        @Test
        @DisplayName("same scorelines in another notation are accepted")
        void sameScorelinesAccepted() {
            assertTrue(ChronicleValidator.translationProblems(SOURCE,
                    "Dinamo Zagreb pobijedio je 2:1 nakon vodstva od 1:0 na odmoru u 18:00.").isEmpty());
        }

        @Test
        @DisplayName("a scoreline that changed in translation is rejected")
        void changedScorelineRejected() {
            assertEquals(1, ChronicleValidator.translationProblems(SOURCE,
                    "Dinamo Zagreb pobijedio je 3:1 nakon vodstva od 1:0.").size());
        }

        @Test
        @DisplayName("a scoreline lost in translation is rejected")
        void lostScorelineRejected() {
            assertEquals(1, ChronicleValidator.translationProblems(SOURCE,
                    "Dinamo Zagreb je pobijedio nakon vodstva od 1:0.").size());
        }

        @Test
        @DisplayName("empty or note-polluted translations are rejected")
        void emptyOrPolluted() {
            assertEquals(1, ChronicleValidator.translationProblems(SOURCE, "").size());
            assertTrue(ChronicleValidator.translationProblems(SOURCE,
                    "Dinamo je pobijedio 2:1 nakon 1:0. Here is the translation:").size() >= 1);
        }
    }
}
