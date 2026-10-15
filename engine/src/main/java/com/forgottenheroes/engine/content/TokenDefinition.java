package com.forgottenheroes.engine.content;

import com.forgottenheroes.engine.model.Stats;

/** A unit an ability summons; never for sale in the tavern (GDD §5, Tokens). */
public record TokenDefinition(String id, String name, int attack, int health, String sprite) {

    public TokenDefinition {
        Ids.require(id, "token");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("token " + id + " needs a name");
        }
        new Stats(attack, health);
    }

    public Stats stats() {
        return new Stats(attack, health);
    }
}
