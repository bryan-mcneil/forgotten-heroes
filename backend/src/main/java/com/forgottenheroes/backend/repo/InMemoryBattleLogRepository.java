package com.forgottenheroes.backend.repo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Battle logs in a map keyed by run and turn, gone when the app stops. */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryBattleLogRepository implements BattleLogRepository {

    private final Map<String, BattleLog> logs = new ConcurrentHashMap<>();

    @Override
    public void save(BattleLog log) {
        logs.put(key(log.runId(), log.turn()), log);
    }

    @Override
    public Optional<BattleLog> find(String runId, int turn) {
        return Optional.ofNullable(logs.get(key(runId, turn)));
    }

    private static String key(String runId, int turn) {
        return runId + "/" + turn;
    }
}
