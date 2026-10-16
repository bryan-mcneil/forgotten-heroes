package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.session.BandSnapshot;
import java.time.Instant;

/** The band a player ended turn {@code turn} with, kept for others to fight (architecture §4.4, §5). */
public record Ghost(String ownerUserId, int turn, int crowns, int hearts, BandSnapshot band, boolean bot, Instant savedAt) {
}
