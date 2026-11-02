package com.league.sportsleague.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI sportsLeagueOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sports League Management API")
                        .description("P39 — REST API for managing sports leagues, seasons, teams, fixtures, and match events.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Doalto Cavada Bienvenido")
                                .email("futbolero3amistad@gmail.com"))
                        .license(new License()
                                .name("Academic — Algebra Bernays University 2026/2027")));
    }
}
