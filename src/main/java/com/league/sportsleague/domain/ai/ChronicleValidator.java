package com.league.sportsleague.domain.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks an AI answer against the data it was written from, before anything is stored or shown. The model
 * is never trusted with facts: every scoreline in the text must be one the data supports, the final score
 * must be stated, and nothing but the chronicle may be in the answer (some models leak drafting notes such
 * as "Count is 146. Need 150 - 200 words."). A returned list of problems is empty when the answer is fine;
 * the problems are written so they can be fed back to the model for one corrected attempt.
 */
public final class ChronicleValidator {

    static final int MIN_WORDS = 100;
    static final int MAX_WORDS = 260;

    /** n-n, n–n or n:n with numbers of 1-3 digits; not part of a longer number, decimal, date or fraction. */
    private static final Pattern SCORELINE = Pattern.compile(
            "(?<![\\d:/])(?<!\\d[.,])(?<!\\d-)(\\d{1,3})\\s*([-\u2013:])\\s*(\\d{1,3})(?!\\d)(?![.,]\\d)(?![:/])");

    /** Signs that the model wrote about its own work instead of (or next to) the chronicle. */
    private static final Pattern NOTES = Pattern.compile(
            "(?i)\\bword count\\b|\\bcount is\\b|\\bneeds? \\d+|\\bdraft\\b|\\blet me\\b|\\bas an ai\\b|"
                    + "\\bhere('s| is| are) (the|a|your|my)\\b|\\bnote:|\\bbroj ri\u010di\\b|\\bevo (je )?(tvoj|vas|va\u0161)\\b|"
                    + "\\(\\d{2,3}\\)|\\*\\*|^\\s*#|\\b\\d+\\s*(-|to|\u2013)\\s*\\d+\\s+words\\b");

    private ChronicleValidator() {
    }

    /** Problems of a freshly generated chronicle for {@code request}; empty when it can be published. */
    public static List<String> problems(ChronicleRequest request, String text) {
        List<String> problems = new ArrayList<>();
        if (text == null || text.isBlank()) {
            problems.add("The answer is empty");
            return problems;
        }
        if (NOTES.matcher(text).find()) {
            problems.add("The answer contains notes or drafting text (word counts, markdown or comments); "
                    + "output only the chronicle");
        }
        int words = text.trim().split("\\s+").length;
        if (words < MIN_WORDS || words > MAX_WORDS) {
            problems.add("The answer has %d words, between 150 and 200 are required".formatted(words));
        }

        Set<String> allowed = allowedScorelines(request);
        String finalScore = request.homeScore() + "-" + request.awayScore();
        Set<String> stated = scorelines(text, allowed);
        if (!stated.contains(finalScore)) {
            problems.add("The final score %s (home team first) is not stated".formatted(finalScore));
        }
        for (String scoreline : stated) {
            if (!allowed.contains(scoreline)) {
                problems.add("The scoreline %s is not in the data; use only %s".formatted(scoreline, allowed));
            }
        }
        return problems;
    }

    /** Problems of a translation of {@code source}: it must keep every scoreline and add none. */
    public static List<String> translationProblems(String source, String translated) {
        List<String> problems = new ArrayList<>();
        if (translated == null || translated.isBlank()) {
            problems.add("The translation is empty");
            return problems;
        }
        if (NOTES.matcher(translated).find()) {
            problems.add("The translation contains notes or drafting text; output only the translated text");
        }
        Set<String> original = scorelines(source, Set.of());
        Set<String> kept = scorelines(translated, Set.of());
        if (!original.equals(kept)) {
            problems.add("The translation changed the scorelines: expected %s but found %s".formatted(original, kept));
        }
        return problems;
    }

    /** The scores the data supports: the running score after each goal (from 0-0) and the final score. */
    private static Set<String> allowedScorelines(ChronicleRequest request) {
        Set<String> allowed = new LinkedHashSet<>();
        int home = 0;
        int away = 0;
        allowed.add(home + "-" + away);
        List<MatchEventSummary> ordered = request.events().stream()
                .sorted(Comparator.comparingInt(MatchEventSummary::minute)).toList();
        for (MatchEventSummary e : ordered) {
            boolean forHome = request.homeTeam().equals(e.teamName());
            switch (e.eventType()) {
                case "GOAL", "PENALTY" -> { if (forHome) home++; else away++; }
                case "OWN_GOAL" -> { if (forHome) away++; else home++; }
                default -> { }
            }
            allowed.add(home + "-" + away);
        }
        allowed.add(request.homeScore() + "-" + request.awayScore());
        return allowed;
    }

    /**
     * Every distinct "a-b" scoreline of the text, normalised to a hyphen. A colon pair that looks like a clock
     * time (18:00) is not a scoreline unless the data really has such a score.
     */
    private static Set<String> scorelines(String text, Set<String> allowed) {
        Set<String> found = new TreeSet<>();
        Matcher m = SCORELINE.matcher(text);
        while (m.find()) {
            int a = Integer.parseInt(m.group(1));
            int b = Integer.parseInt(m.group(3));
            String key = a + "-" + b;
            boolean clockLike = ":".equals(m.group(2)) && m.group(3).length() == 2 && a <= 23 && b <= 59;
            if (clockLike && !allowed.contains(key)) {
                continue;
            }
            found.add(key);
        }
        return found;
    }
}
