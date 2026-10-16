package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.service.RunService;
import com.forgottenheroes.backend.web.dto.ActionRequest;
import com.forgottenheroes.backend.web.dto.ActionResponse;
import com.forgottenheroes.backend.web.dto.BattleDto;
import com.forgottenheroes.backend.web.dto.EndTurnRequest;
import com.forgottenheroes.backend.web.dto.EndTurnResponse;
import com.forgottenheroes.backend.web.dto.RunStateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The run routes of architecture §4.2. A controller only translates HTTP into a service call: it
 * reads the path and the body, adds who is calling, hands them to {@link RunService}, and returns the
 * DTO as JSON. Every rule lives in the engine; every error becomes a ProblemDetail in
 * {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runs;
    private final CurrentUser user;

    public RunController(RunService runs, CurrentUser user) {
        this.runs = runs;
        this.user = user;
    }

    /** Start a run; 409 if one is already active. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunStateDto start() {
        return runs.start(user.id());
    }

    @GetMapping("/{runId}")
    public RunStateDto get(@PathVariable String runId) {
        return runs.get(user.id(), runId);
    }

    /** One tavern action; {@code @Valid} runs the constraints of {@link ActionRequest} before this method. */
    @PostMapping("/{runId}/actions")
    public ActionResponse act(@PathVariable String runId, @Valid @RequestBody ActionRequest request) {
        return runs.apply(user.id(), runId, request);
    }

    @PostMapping("/{runId}/end-turn")
    public EndTurnResponse endTurn(@PathVariable String runId, @Valid @RequestBody EndTurnRequest request) {
        return runs.endTurn(user.id(), runId, request.expectedVersion());
    }

    @GetMapping("/{runId}/battles/{turn}")
    public BattleDto battle(@PathVariable String runId, @PathVariable int turn) {
        return runs.battle(user.id(), runId, turn);
    }

    @PostMapping("/{runId}/abandon")
    public RunStateDto abandon(@PathVariable String runId) {
        return runs.abandon(user.id(), runId);
    }
}
