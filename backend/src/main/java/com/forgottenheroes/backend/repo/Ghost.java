package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.session.BandSnapshot;
import java.time.Instant;

/**
 * The band a player ended turn {@code turn} with, kept for others to fight (architecture §4.4, §5).
 * The run id is part of the ghost's sort key in DynamoDB, so two ghosts saved in the same millisecond
 * with the same crowns still get two folders.
 */
public record Ghost(String ownerUserId, String runId, int turn, int crowns, int hearts, BandSnapshot band, boolean bot,
                    Instant savedAt) {
}
