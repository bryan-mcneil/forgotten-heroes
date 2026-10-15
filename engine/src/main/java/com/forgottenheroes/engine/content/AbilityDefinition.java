package com.forgottenheroes.engine.content;

import java.util.List;

/** A hero's ability: when it fires, what the player reads, what it does, and how often it may fire. */
public record AbilityDefinition(Trigger trigger, String text, List<EffectDefinition> effects, Limit limit) {

    public AbilityDefinition {
        if (trigger == null) {
            throw new IllegalArgumentException("ability needs a trigger");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("ability needs a text");
        }
        if (effects == null || effects.isEmpty()) {
            throw new IllegalArgumentException("ability needs at least one effect");
        }
        effects = List.copyOf(effects);
        if (limit == null) {
            limit = Limit.NONE;
        }
    }
}
