package com.league.sportsleague.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nBundlesTest {

    private static final List<String> AREAS =
            List.of("common", "pages", "leagues", "teams", "seasons", "fixtures", "validation");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Pattern LITERAL_KEY = Pattern.compile("#\\{([A-Za-z][A-Za-z0-9_.]*)[})(]");

    private static Properties load(String name) throws IOException {
        Properties p = new Properties();
        try (InputStream in = I18nBundlesTest.class.getResourceAsStream("/i18n/" + name);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            p.load(reader);
        }
        return p;
    }

    @Test
    @DisplayName("every area has the same keys in English and Croatian, and no empty values")
    void englishAndCroatianBundlesMatch() throws IOException {
        for (String area : AREAS) {
            Properties en = load(area + ".properties");
            Properties hr = load(area + "_hr.properties");
            assertEquals(new TreeSet<>(en.stringPropertyNames()), new TreeSet<>(hr.stringPropertyNames()),
                    "key mismatch between EN and HR in area '" + area + "'");
            en.forEach((k, v) -> assertTrue(!v.toString().isBlank(), "empty EN value: " + k));
            hr.forEach((k, v) -> assertTrue(!v.toString().isBlank(), "empty HR value: " + k));
        }
    }

    @Test
    @DisplayName("every literal #{key} used in a template exists in the English bundles")
    void templateKeysExist() throws IOException {
        Set<String> known = new TreeSet<>();
        for (String area : AREAS) known.addAll(load(area + ".properties").stringPropertyNames());

        Set<String> missing = new TreeSet<>();
        try (Stream<Path> files = Files.walk(TEMPLATES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                Matcher m = LITERAL_KEY.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    if (!known.contains(m.group(1))) missing.add(file.getFileName() + " -> " + m.group(1));
                }
            }
        }
        assertTrue(missing.isEmpty(), "message keys used in templates but not defined: " + missing);
    }
}
