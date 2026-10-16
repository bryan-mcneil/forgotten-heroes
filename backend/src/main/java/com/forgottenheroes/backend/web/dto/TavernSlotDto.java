package com.forgottenheroes.backend.web.dto;

/** One shelf slot as the client sees it; {@code itemId} is null when the slot is empty. */
public record TavernSlotDto(String itemId, int price, int bonusAttack, int bonusHealth, boolean frozen) {
}
