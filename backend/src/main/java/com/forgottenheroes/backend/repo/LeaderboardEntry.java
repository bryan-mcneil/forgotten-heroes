package com.forgottenheroes.backend.repo;

import java.time.Instant;

/**
 * One finished run on a season's board (architecture §5, the LB# drawer). The display name is copied
 * here when the run ends: the board is read far more often than a name changes, and DynamoDB has no
 * join to fetch it with, so a rename shows on new rows only. {@code season} is the month the run
 * finished in, as {@code yyyy-MM} in UTC.
 */
public record LeaderboardEntry(String season, String userId, String displayName, int crowns, int turns, Instant finishedAt) {
}
