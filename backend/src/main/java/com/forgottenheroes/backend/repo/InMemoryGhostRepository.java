package com.forgottenheroes.backend.repo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Ghosts in a map keyed by turn, gone when the app stops. */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryGhostRepository implements GhostRepository {

    private final Map<Integer, List<Ghost>> byTurn = new ConcurrentHashMap<>();

    @Override
    public void save(Ghost ghost) {
        byTurn.computeIfAbsent(ghost.turn(), turn -> new CopyOnWriteArrayList<>()).add(ghost);
    }

    @Override
    public List<Ghost> findByTurn(int turn) {
        return List.copyOf(byTurn.getOrDefault(turn, List.of()));
    }
}
