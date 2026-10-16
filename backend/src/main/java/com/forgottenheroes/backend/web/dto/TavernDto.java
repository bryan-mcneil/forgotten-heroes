package com.forgottenheroes.backend.web.dto;

import java.util.List;

/** The shelves: hero row, relic row and the optional bonus slot (null when closed). */
public record TavernDto(List<TavernSlotDto> heroes, List<TavernSlotDto> relics, TavernSlotDto bonusSlot) {
}
