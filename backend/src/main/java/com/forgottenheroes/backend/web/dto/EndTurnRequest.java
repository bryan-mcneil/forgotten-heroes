package com.forgottenheroes.backend.web.dto;

import jakarta.validation.constraints.NotNull;

/** The body of end-turn: only the version the client last saw, so a stale client gets 409 here too. */
public record EndTurnRequest(@NotNull Long expectedVersion) {
}
