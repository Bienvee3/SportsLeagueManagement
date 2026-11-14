package com.league.sportsleague.infrastructure.security;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        // /actuator/health is the hosting platform's liveness probe; it exposes only the status
                        // (management.endpoint.health.show-details=never) and no other actuator endpoint is exposed.
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error",
                                "/actuator/health", "/actuator/health/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/api-docs").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/official/**").hasAnyRole("ADMIN", "OFFICIAL")
                        .anyRequest().hasAnyRole("ADMIN", "OFFICIAL", "VIEWER"))
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    @Bean
    UserDetailsManager userDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
