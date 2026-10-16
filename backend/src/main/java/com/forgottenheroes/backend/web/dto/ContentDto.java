package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.content.RelicDefinition;
import com.forgottenheroes.engine.content.TokenDefinition;
import java.util.List;

/** Everything the client needs to draw the game: the content files as the engine loaded them. */
public record ContentDto(List<HeroDefinition> heroes, List<RelicDefinition> relics, List<TokenDefinition> tokens) {
}
