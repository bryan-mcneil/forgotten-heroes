package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.model.RunState;
import java.util.Optional;

/**
 * Where runs live. The service only knows these two calls; what is behind them (a map today,
 * DynamoDB from Step 28) is the repository's business. A run is always looked up with its owner, so
 * one player can never load another's run.
 */
public interface RunRepository {

    Optional<RunState> find(String userId, String runId);

    void save(String userId, RunState state);
}
