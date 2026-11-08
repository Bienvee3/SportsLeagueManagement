package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChroniclePort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the active {@link ChroniclePort} implementation.
 *
 * <ul>
 *   <li>When {@code spring.ai.model.chat=google-genai}: {@link GeminiChronicleAdapter} (real Gemini API).</li>
 *   <li>Otherwise (e.g. tests, {@code spring.ai.model.chat=none}): {@link StubChronicleAdapter} (deterministic fallback).</li>
 * </ul>
 */
@Configuration
public class ChronicleAdapterConfig {

    /**
     * Gemini client with explicit limits. Spring AI 2.0.2 exposes no timeout or retry property for Google
     * GenAI ({@code spring.ai.google.genai.*} only has credentials and chat options) and its auto-configured
     * client uses the SDK defaults, which retry with backoff: with the defaults a stuck provider kept the
     * caller waiting well past the timeout (measured: ~14 s for a 1 ms timeout). Replaces the auto-configured
     * client (which is {@code @ConditionalOnMissingBean}); API-key mode only.
     *
     * <ul>
     *   <li>{@code app.ai.timeout-ms}: per-attempt timeout for connecting, writing and reading.</li>
     *   <li>{@code app.ai.max-attempts}: total attempts including the first (1 = no retry), so the worst
     *       case is about {@code timeout-ms * max-attempts} plus the SDK's short backoff.</li>
     * </ul>
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
    Client googleGenAiClientWithTimeout(@Value("${spring.ai.google.genai.api-key}") String apiKey,
                                        @Value("${app.ai.timeout-ms:30000}") int timeoutMs,
                                        @Value("${app.ai.max-attempts:2}") int maxAttempts) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(timeoutMs)
                        .retryOptions(HttpRetryOptions.builder().attempts(maxAttempts).build())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
    ChroniclePort geminiChroniclePort(ChatClient.Builder chatClientBuilder) {
        return new GeminiChronicleAdapter(chatClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(ChroniclePort.class)
    ChroniclePort stubChroniclePort() {
        return new StubChronicleAdapter();
    }
}
