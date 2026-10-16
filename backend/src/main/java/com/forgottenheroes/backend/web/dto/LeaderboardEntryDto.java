package com.forgottenheroes.backend.web.dto;

import java.time.Instant;

/** One leaderboard row; ranked by crowns, then fewest turns (architecture §4.2). */
public record LeaderboardEntryDto(int rank, String displayName, int crowns, int turns, Instant finishedAt) {
}
