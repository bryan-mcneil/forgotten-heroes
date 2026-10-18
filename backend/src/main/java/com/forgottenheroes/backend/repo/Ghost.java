package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.session.BandSnapshot;
import java.time.Instant;

/**
 * The band a player ended turn {@code turn} with, kept for others to fight (architecture §4.4, §5).
 * The run id is part of the ghost's sort key in DynamoDB, so two ghosts saved in the same millisecond
 * with the same crowns still get two folders. {@code ownerName} is the owner's display name copied in
 * at save time (Step 32): the battle screen reads it straight off the ghost, no second lookup, and a
 * later rename shows on the owner's later ghosts only.
 */
public record Ghost(String ownerUserId, String ownerName, String runId, int turn, int crowns, int hearts, BandSnapshot band,
                    boolean bot, Instant savedAt) {

    /** A ghost whose owner has no display name to show: the id stands in (bots, tests, folders saved before Step 32). */
    public Ghost(String ownerUserId, String runId, int turn, int crowns, int hearts, BandSnapshot band, boolean bot, Instant savedAt) {
        this(ownerUserId, ownerUserId, runId, turn, crowns, hearts, band, bot, savedAt);
    }
}
