package com.forgottenheroes.backend.repo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Rows in a map per season, gone when the app stops; sorted on read the way the DynamoDB sort key orders them. */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryLeaderboardRepository implements LeaderboardRepository {

    /** Highest crowns first, fewest turns within, then the player: the leaderboard sort key, read forwards. */
    private static final Comparator<LeaderboardEntry> RANK = Comparator.comparingInt(LeaderboardEntry::crowns).reversed()
        .thenComparingInt(LeaderboardEntry::turns)
        .thenComparing(LeaderboardEntry::userId);

    private final Map<String, Map<String, LeaderboardEntry>> bySeason = new ConcurrentHashMap<>();

    @Override
    public void save(LeaderboardEntry entry) {
        // the same key the ledger would use, so a repeated result replaces its row here too
        String key = entry.userId() + "#" + entry.crowns() + "#" + entry.turns();
        bySeason.computeIfAbsent(entry.season(), season -> new ConcurrentHashMap<>()).put(key, entry);
    }

    @Override
    public List<LeaderboardEntry> top(String season, int limit) {
        return bySeason.getOrDefault(season, Map.of()).values().stream().sorted(RANK).limit(limit).toList();
    }
}
