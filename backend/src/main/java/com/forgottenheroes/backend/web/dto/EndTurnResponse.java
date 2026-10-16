package com.forgottenheroes.backend.web.dto;

/** What ending a turn hands back: the battle, then the state with the next turn already begun. */
public record EndTurnResponse(BattleDto battle, RunStateDto state) {
}
