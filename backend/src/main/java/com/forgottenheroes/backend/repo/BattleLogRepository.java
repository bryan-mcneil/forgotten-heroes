package com.forgottenheroes.backend.repo;

import java.util.Optional;

/** Past battles of a run, one per turn, for {@code GET /runs/{runId}/battles/{turn}}. */
public interface BattleLogRepository {

    void save(BattleLog log);

    Optional<BattleLog> find(String runId, int turn);
}
