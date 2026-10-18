package com.forgottenheroes.backend.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness check: proves the app is up and says which commit is running. No auth (SecurityConfig.PUBLIC). */
@RestController
public class HealthController {

    /** The JSON body of GET /api/health. */
    public record HealthResponse(String status, String version) {}

    private final GitProperties git;

    public HealthController(GitProperties git) {
        this.git = git;
    }

    // the empty list tells the OpenAPI contract this door needs no token, unlike the rest of the API
    @SecurityRequirements
    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", git.getShortCommitId());
    }
}
