package com.forgottenheroes.backend.repo;

import java.util.List;

/** A season's results: write one row when a run ends, read the best {@code limit} rows in rank order. */
public interface LeaderboardRepository {

    /** Adds the row; the same player with the same crowns and turns replaces their earlier row. */
    void save(LeaderboardEntry entry);

    /** Highest crowns first, fewest turns within equal crowns, at most {@code limit} rows; the sort key does the ranking. */
    List<LeaderboardEntry> top(String season, int limit);
}
