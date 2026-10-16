package com.forgottenheroes.backend.repo;

import java.time.Instant;

/** A player as stored (architecture §5, the PROFILE item). Immutable: every change is a new copy. */
public record Profile(String userId, String displayName, Instant createdAt, int bestCrowns, int runsPlayed, String activeRunId) {

    /** First call of {@code /me}: the id doubles as the name until Step 31 brings the Cognito one. */
    public static Profile create(String userId, Instant now) {
        return new Profile(userId, userId, now, 0, 0, null);
    }

    public Profile withActiveRun(String runId) {
        return new Profile(userId, displayName, createdAt, bestCrowns, runsPlayed, runId);
    }

    /** A run ended (won, lost or abandoned): count it, keep the best, free the slot. */
    public Profile runFinished(int crowns) {
        return new Profile(userId, displayName, createdAt, Math.max(bestCrowns, crowns), runsPlayed + 1, null);
    }
}
