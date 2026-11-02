package com.league.sportsleague.infrastructure.ai;

import com.league.sportsleague.domain.ai.ChroniclePort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
