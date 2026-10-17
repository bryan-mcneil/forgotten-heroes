package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.json.RunStateJson;
import com.forgottenheroes.engine.model.RunState;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * Runs in a map, gone when the app stops. It stores the run as JSON text, exactly as DynamoDB will
 * (Step 28), so a caller always gets a fresh object back, never the one it saved: a bug that mutates
 * state after saving shows up here, not in production.
 */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryRunRepository implements RunRepository {

    private final Map<String, String> runsJson = new ConcurrentHashMap<>();

    @Override
    public Optional<RunState> find(String userId, String runId) {
        return Optional.ofNullable(runsJson.get(key(userId, runId))).map(RunStateJson::fromJson);
    }

    @Override
    public void save(String userId, RunState state) {
        runsJson.put(key(userId, state.runId()), RunStateJson.toJson(state));
    }

    private static String key(String userId, String runId) {
        return userId + "/" + runId;
    }
}
