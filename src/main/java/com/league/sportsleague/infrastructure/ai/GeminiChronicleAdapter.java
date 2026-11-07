package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChroniclePort;
import com.league.sportsleague.domain.ai.ChronicleRequest;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.ai.MatchEventSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import com.league.sportsleague.domain.exception.ChronicleUnavailableException;

/**
 * Outbound adapter: calls Gemini (Google GenAI) via Spring AI ChatClient.
 * Registered as a Spring bean only when {@code spring.ai.model.chat=google-genai}
 * — see {@link ChronicleAdapterConfig}.
 * The prompt is always written in English; output language is controlled by the request.
 */
public class GeminiChronicleAdapter implements ChroniclePort {

    private static final Logger log = LoggerFactory.getLogger(GeminiChronicleAdapter.class);

    private final ChatClient chatClient;

    public GeminiChronicleAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public ChronicleResult generate(ChronicleRequest request) {
        String text = call(buildPrompt(request));
        return new ChronicleResult(text, request.language());
    }

    @Override
    public ChronicleResult translate(ChronicleResult source, Language target) {
        String prompt = "Translate the following football match chronicle into %s. Keep every name, number, "
                .formatted(languageInstruction(target))
                + "score and fact exactly as written, keep the paragraph structure, add nothing and omit "
                + "nothing. Output only the translated text.\n\nCHRONICLE:\n" + source.text();
        return new ChronicleResult(call(prompt), target);
    }

    private String call(String prompt) {
        try {
            String text = chatClient.prompt(prompt).call().content();
            if (text == null || text.isBlank()) {
                throw new ChronicleUnavailableException("The AI provider returned an empty answer");
            }
            return text;
        } catch (ChronicleUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            log.warn("Gemini API call failed: {} (root cause: {})", ex.getMessage(), root.getMessage());
            throw new ChronicleUnavailableException("The AI provider is temporarily unavailable: " + root.getMessage(), ex);
        }
    }

    // ── Prompt builder ────────────────────────────────────────────────────────

    private static String buildPrompt(ChronicleRequest req) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a professional sports journalist. ");
        sb.append("Write a concise post-match chronicle of exactly 150-200 words based strictly ");
        sb.append("on the data provided below. Do not invent details not present in the data.\n\n");

        sb.append("MATCH DATA:\n");
        sb.append("Home team : ").append(req.homeTeam()).append("\n");
        sb.append("Away team : ").append(req.awayTeam()).append("\n");
        sb.append("Final score: ").append(req.homeScore()).append("-").append(req.awayScore())
          .append(" (").append(resultLabel(req)).append(")\n");

        if (!req.events().isEmpty()) {
            sb.append("\nMATCH EVENTS (chronological):\n");
            for (MatchEventSummary e : req.events()) {
                sb.append("  ").append(e.minute()).append("' — ")
                  .append(formatEventType(e.eventType()))
                  .append(" — ").append(e.teamName());
                if (e.playerName() != null && !e.playerName().isBlank()) {
                    sb.append(" (").append(e.playerName()).append(")");
                }
                sb.append("\n");
            }
        } else {
            sb.append("\nNo individual events recorded.\n");
        }

        sb.append("\nOUTPUT REQUIREMENTS:\n");
        sb.append("- Language: ").append(languageInstruction(req.language())).append("\n");
        sb.append("- Tone: professional journalistic\n");
        sb.append("- Length: 150-200 words\n");
        sb.append("- Include: match narrative, score, key events\n");
        sb.append("- Exclude: fictional details, speculation\n");

        return sb.toString();
    }

    private static String resultLabel(ChronicleRequest req) {
        if (req.homeScore() > req.awayScore()) return req.homeTeam() + " win";
        if (req.homeScore() < req.awayScore()) return req.awayTeam() + " win";
        return "draw";
    }

    private static String formatEventType(String raw) {
        return switch (raw) {
            case "GOAL"         -> "Goal";
            case "PENALTY"      -> "Penalty goal";
            case "OWN_GOAL"     -> "Own goal";
            case "YELLOW_CARD"  -> "Yellow card";
            case "RED_CARD"     -> "Red card";
            case "SUBSTITUTION" -> "Substitution";
            default             -> raw;
        };
    }

    private static String languageInstruction(Language lang) {
        return switch (lang) {
            case ENGLISH  -> "English only";
            case CROATIAN -> "Croatian (Hrvatski) only";
        };
    }
}
