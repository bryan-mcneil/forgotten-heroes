package com.forgottenheroes.engine.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.forgottenheroes.engine.model.Rank;
import java.util.List;

/** One row of the GDD §6 relic table. A relic is a perk relic when it gives a perk; instant otherwise. */
public record RelicDefinition(String id, String name, Rank rank, int price, String text, String sprite,
                              List<EffectDefinition> effects) {

    public RelicDefinition {
        Ids.require(id, "relic");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("relic " + id + " needs a name");
        }
        if (rank == null) {
            throw new IllegalArgumentException("relic " + id + " needs a rank");
        }
        if (price < 0) {
            throw new IllegalArgumentException("relic " + id + " price cannot be negative");
        }
        if (effects == null || effects.isEmpty()) {
            throw new IllegalArgumentException("relic " + id + " needs at least one effect");
        }
        effects = List.copyOf(effects);
    }

    @JsonIgnore
    public boolean isPerk() {
        return effects.stream().anyMatch(effect -> effect instanceof EffectDefinition.GivePerk);
    }

    /** True when the player must pick a band member to use it on. */
    public boolean needsChosenTarget() {
        return effects.stream()
            .anyMatch(effect -> effect.target() != null && effect.target().select() == TargetSelect.CHOSEN);
    }
}
