package com.forgottenheroes.backend.repo;

import com.forgottenheroes.engine.model.RunState;
import java.util.Optional;

/**
 * Where runs live. The service only knows these two calls; what is behind them (a map or DynamoDB)
 * is the repository's business. A run is always looked up with its owner, so one player can never
 * load another's run.
 *
 * <p>Every save is conditional (architecture §4.3, Step 29): the caller says which version it
 * loaded, and the write happens only if storage still holds that version, or no run exists under
 * that key yet. Two overlapping saves of one run therefore leave exactly one winner; the other gets
 * {@link VersionConflictException}, and nothing of its work reaches storage.
 */
public interface RunRepository {

    /** The "expected version" of a run that does not exist yet: the write may only create. */
    long NEW_RUN = 0;

    Optional<RunState> find(String userId, String runId);

    /**
     * Writes the whole run if storage still holds {@code expectedVersion} (the version the caller
     * loaded, not the one the engine produced since), or no run with this id exists.
     *
     * @throws VersionConflictException when another write landed first
     */
    void save(String userId, RunState state, long expectedVersion);
}
