package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.model.Perk;

/** One hero in the band as the client draws it; {@code perk} is null when the hero has none. */
public record UnitDto(String id, String heroId, int attack, int health, int level, int xp, Perk perk) {
}
