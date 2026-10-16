package com.forgottenheroes.backend.web;

import org.springframework.boot.info.GitProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness check: proves the app is up and says which commit is running. No auth. */
@RestController
public class HealthController {

    /** The JSON body of GET /api/health. */
    public record HealthResponse(String status, String version) {}

    private final GitProperties git;

    public HealthController(GitProperties git) {
        this.git = git;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", git.getShortCommitId());
    }
}
