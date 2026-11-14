package com.league.sportsleague.infrastructure.config;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bean Validation messages must follow the app language: the constraint annotations reference
 * {@code {validation.*}} keys, which the validator resolves from the same MessageSource as the UI texts.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:validation_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ValidationMessagesTest {

    private static final Path DTOS = Path.of("src/main/java/com/league/sportsleague/application/dto");
    private static final Pattern MESSAGE_ATTRIBUTE = Pattern.compile("message\\s*=\\s*\"([^\"]*)\"");
    private static final Locale CROATIAN = Locale.forLanguageTag("hr");

    @Autowired Validator validator;

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    private static Set<String> messages(Set<ConstraintViolation<Object>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static Properties bundle(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = ValidationMessagesTest.class.getResourceAsStream("/i18n/" + name);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return p;
    }

    @Test
    @DisplayName("a blank team name is reported in English by default")
    void englishByDefault() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        Set<String> m = messages(validator.validate((Object) new CreateTeamRequest("", "DIN", "FOOTBALL", "Maksimir", "Zagreb", null)));

        assertEquals(Set.of("Team name must not be blank"), m);
    }

    @Test
    @DisplayName("the same violation is reported in Croatian when the app language is Croatian")
    void croatianWhenAppLanguageIsCroatian() {
        LocaleContextHolder.setLocale(CROATIAN);

        Set<String> m = messages(validator.validate((Object) new CreateTeamRequest("", "DIN", "FOOTBALL", "Maksimir", "Zagreb", null)));

        assertEquals(Set.of("Naziv kluba je obavezan"), m);
    }

    @Test
    @DisplayName("constraint limits are substituted into the translated message")
    void limitsAreSubstituted() {
        LocaleContextHolder.setLocale(CROATIAN);

        Set<String> season = messages(validator.validate((Object) new CreateSeasonRequest("2026", 1800, 2027)));
        Set<String> player = messages(validator.validate((Object) new PlayerRequest("Marko", 100, null)));
        Set<String> result = messages(validator.validate((Object) new RecordResultRequest(-1, 0)));

        assertEquals(Set.of("Početna godina mora biti >= 1900"), season);
        assertEquals(Set.of("Broj dresa mora biti <= 99"), player);
        assertEquals(Set.of("Rezultat domaćina mora biti >= 0"), result);
    }

    @Test
    @DisplayName("every DTO constraint message is a {validation.*} key defined in both languages, and none is unused")
    void everyConstraintUsesADefinedKey() throws IOException {
        Properties en = bundle("validation.properties");
        Properties hr = bundle("validation_hr.properties");
        Set<String> used = new TreeSet<>();
        Set<String> literal = new TreeSet<>();
        try (Stream<Path> files = Files.list(DTOS)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = MESSAGE_ATTRIBUTE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    String text = m.group(1);
                    if (text.matches("\\{validation\\.[A-Za-z0-9.]+}")) {
                        used.add(text.substring(1, text.length() - 1));
                    } else {
                        literal.add(file.getFileName() + " -> " + text);
                    }
                }
            }
        }

        assertTrue(literal.isEmpty(), "constraint messages written as literal text instead of keys: " + literal);
        assertEquals(new TreeSet<>(en.stringPropertyNames()), used, "EN bundle keys and the keys used by the DTOs differ");
        assertEquals(new TreeSet<>(hr.stringPropertyNames()), used, "HR bundle keys and the keys used by the DTOs differ");
        assertTrue(List.copyOf(used).size() > 20);
    }
}
