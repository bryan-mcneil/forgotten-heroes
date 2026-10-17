package com.forgottenheroes.backend.repo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Ghosts in a map keyed by turn, gone when the app stops; sorted on read the way DynamoDB's sort key orders them. */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryGhostRepository implements GhostRepository {

    /** Highest crowns first, newest first among equal crowns, then run id: the ghost sort key, read backwards. */
    private static final Comparator<Ghost> DRAWER_ORDER = Comparator.comparingInt(Ghost::crowns)
        .thenComparing(Ghost::savedAt)
        .thenComparing(Ghost::runId)
        .reversed();

    private final Map<Integer, List<Ghost>> byTurn = new ConcurrentHashMap<>();

    @Override
    public void save(Ghost ghost) {
        byTurn.computeIfAbsent(ghost.turn(), turn -> new CopyOnWriteArrayList<>()).add(ghost);
    }

    @Override
    public List<Ghost> findByTurn(int turn) {
        return byTurn.getOrDefault(turn, List.of()).stream().sorted(DRAWER_ORDER).toList();
    }

    @Override
    public List<Ghost> findCandidates(int turn, int crownsMin, int crownsMax, int limit) {
        return findByTurn(turn).stream()
            .filter(ghost -> ghost.crowns() >= crownsMin && ghost.crowns() <= crownsMax)
            .limit(limit)
            .toList();
    }
}
