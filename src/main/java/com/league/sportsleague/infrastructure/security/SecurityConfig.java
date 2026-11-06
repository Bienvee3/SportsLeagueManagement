package com.league.sportsleague.infrastructure.security;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            UserDetailsService userDetailsService) throws Exception {
        SwitchUserFilter switchUserFilter = new SwitchUserFilter();
        switchUserFilter.setUserDetailsService(userDetailsService);
        switchUserFilter.setSwitchUserUrl("/dev/switch");
        switchUserFilter.setExitUserUrl("/dev/switch-exit");
        switchUserFilter.setTargetUrl("/");

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/error",
                                "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/api-docs").permitAll()
                        .requestMatchers("/dev/switch").hasRole("SWITCH_USER")
                        .requestMatchers("/dev/switch-exit").hasRole("PREVIOUS_ADMINISTRATOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/official/**").hasAnyRole("ADMIN", "OFFICIAL")
                        .anyRequest().hasAnyRole("ADMIN", "OFFICIAL", "VIEWER"))
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll())
                .addFilterAfter(switchUserFilter, AuthorizationFilter.class);
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
