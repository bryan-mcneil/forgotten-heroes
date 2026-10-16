package com.forgottenheroes.backend.web.dto;

import java.util.List;

/** The player's band, front first. */
public record BandDto(List<UnitDto> units) {
}
