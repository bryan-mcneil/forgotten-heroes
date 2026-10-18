package com.forgottenheroes.backend.repo;

import java.time.Instant;

/** A player as stored (architecture §5, the PROFILE item). Immutable: every change is a new copy. */
public record Profile(String userId, String displayName, Instant createdAt, int bestCrowns, int runsPlayed, String activeRunId) {

    /** First call of {@code /me}: a placeholder name made from the id ("Hero e498") until the player picks one with {@code PUT /api/me}. */
    public static Profile create(String userId, Instant now) {
        return new Profile(userId, defaultName(userId), now, 0, 0, null);
    }

    /** "Hero " plus the first four letters or digits of the id: short, unique enough, and valid under the naming rules. */
    static String defaultName(String userId) {
        String tail = userId.replaceAll("[^A-Za-z0-9]", "");
        return "Hero " + tail.substring(0, Math.min(4, tail.length()));
    }

    public Profile withDisplayName(String name) {
        return new Profile(userId, name, createdAt, bestCrowns, runsPlayed, activeRunId);
    }

    public Profile withActiveRun(String runId) {
        return new Profile(userId, displayName, createdAt, bestCrowns, runsPlayed, runId);
    }

    /** A run ended (won, lost or abandoned): count it, keep the best, free the slot. */
    public Profile runFinished(int crowns) {
        return new Profile(userId, displayName, createdAt, Math.max(bestCrowns, crowns), runsPlayed + 1, null);
    }
}
