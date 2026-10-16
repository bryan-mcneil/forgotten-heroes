package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.model.Outcome;

/** The last battle in three numbers, for the run screen; null before the first battle. */
public record BattleSummaryDto(int turn, Outcome outcome, int clashes) {
}
