package com.league.sportsleague.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * App language (English / Croatian). Chosen with {@code ?appLang=en|hr} (not {@code lang}, which the
 * chronicle REST endpoint already uses), remembered in the {@code app-lang} cookie. It translates the
 * UI texts and selects the language of AI match chronicles.
 */
@Configuration
public class LanguageConfig implements WebMvcConfigurer {

    @Bean
    LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("app-lang");
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    /**
     * UI texts: one bundle pair per area under {@code i18n/} (English base + {@code _hr}).
     * A missing key renders as the key itself instead of failing the page.
     */
    @Bean
    MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("i18n/common", "i18n/pages", "i18n/leagues", "i18n/teams",
                "i18n/seasons", "i18n/fixtures", "i18n/validation");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("appLang");
        registry.addInterceptor(interceptor);
    }
}
