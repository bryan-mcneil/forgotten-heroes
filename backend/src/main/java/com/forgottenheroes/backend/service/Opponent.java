package com.forgottenheroes.backend.service;

import com.forgottenheroes.engine.session.BandSnapshot;

/** Who the player fights this turn: a display name for the battle screen and the band. */
public record Opponent(String name, BandSnapshot band) {
}
