package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.model.RunStatus;

/**
 * The run as the client sees it. No seed (it would reveal future rolls), no engine bookkeeping;
 * {@code version} comes back as {@code expectedVersion} on every action.
 */
public record RunStateDto(
    String runId,
    int turn,
    int gold,
    int hearts,
    int crowns,
    RunStatus status,
    boolean freeRollAvailable,
    BandDto band,
    TavernDto tavern,
    BattleSummaryDto lastBattle,
    long version,
    int lastEventSeq) {
}
