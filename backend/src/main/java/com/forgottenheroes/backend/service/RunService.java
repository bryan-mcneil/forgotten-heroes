package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.BattleDto;
import com.forgottenheroes.backend.web.dto.EndTurnResponse;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import org.springframework.stereotype.Service;

/**
 * Everything a run can do, from the API's point of view. Step 26 only fixes the shapes: every method
 * says "not implemented" (501). Step 27 drives the engine through {@code GameSession} behind these
 * same signatures, so the controllers and their tests do not change.
 */
@Service
public class RunService {

    public RunStateDto start() {
        throw notYet();
    }

    public RunStateDto get(String runId) {
        throw notYet();
    }

    public ActionResponse apply(String runId, ActionRequest request) {
        throw notYet();
    }

    public EndTurnResponse endTurn(String runId, long expectedVersion) {
        throw notYet();
    }

    public BattleDto battle(String runId, int turn) {
        throw notYet();
    }

    public RunStateDto abandon(String runId) {
        throw notYet();
    }

    private static UnsupportedOperationException notYet() {
        return new UnsupportedOperationException("arrives in Step 27");
    }
}
