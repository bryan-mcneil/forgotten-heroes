package com.forgottenheroes.backend.repo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

/** Ghosts in a map keyed by turn, gone when the app stops. */
@Repository
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
