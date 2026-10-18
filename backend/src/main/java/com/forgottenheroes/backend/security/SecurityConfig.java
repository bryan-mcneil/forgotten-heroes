package com.forgottenheroes.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * The doorman (architecture §7). Every request but the public ones must carry a Cognito access token
 * as {@code Authorization: Bearer <token>}. Spring Security's resource server pulls the token out,
 * checks it with {@link #jwtDecoder} and puts the result in the security context, where
 * {@link SecurityContextCurrentUser} reads the player's id. Not loaded under the {@code local} profile:
 * there {@code security.local} lets everyone in and reads a header instead.
 */
@Configuration(proxyBeanMethods = false)
@Profile("!local")
public class SecurityConfig {

    /** The doors anyone may open without a card: liveness, the content files, and the API description (off in prod anyway). */
    static final String[] PUBLIC = {"/api/health", "/api/content", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};

    @Bean
    SecurityFilterChain api(HttpSecurity http, ProblemDetailAuthenticationEntryPoint entryPoint) throws Exception {
        return http
            // no browser session, no cookie: each request proves itself with the token it carries
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // CSRF guards cookie sessions; a bearer token is never attached by the browser on its own, so there is nothing to forge
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(doors -> doors
                .requestMatchers(PUBLIC).permitAll()
                .anyRequest().authenticated())
            // read the Bearer token, run it through the decoder, and answer 401 as a ProblemDetail when it fails
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(entryPoint))
            // the same 401 body when there is no token at all
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
            .build();
    }

    @Bean
    ProblemDetailAuthenticationEntryPoint problemDetailEntryPoint(JsonMapper json) {
        return new ProblemDetailAuthenticationEntryPoint(json);
    }

    /**
     * How a card is checked, in order: the signature with Cognito's public keys (fetched once from the
     * pool's {@code jwks.json}, then cached), the issuer, the expiry, and the kind of token. Cognito hands
     * out an access token and an id token; only the access token is for APIs, so the id token is refused.
     */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer),
            new JwtClaimValidator<String>("token_use", "access"::equals)));
        return decoder;
    }
}
