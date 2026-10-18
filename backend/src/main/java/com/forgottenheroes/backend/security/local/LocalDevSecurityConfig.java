package com.forgottenheroes.backend.security.local;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * The doorman's laptop rules: no Cognito, no token. Every request is let in and the player is whoever
 * the {@code X-Dev-User} header names ({@code dev} when absent). Only the {@code local} profile loads this
 * class, and the backend's {@code ArchitectureTest} proves nothing outside this package refers to it, so
 * the shortcut cannot leak into the cloud (architecture §7, tech stack §10).
 */
@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalDevSecurityConfig {

    @Bean
    SecurityFilterChain localApi(HttpSecurity http) throws Exception {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(doors -> doors.anyRequest().permitAll())
            .addFilterBefore(new DevUserHeaderFilter(), AuthorizationFilter.class)
            .build();
    }
}
