package com.forgottenheroes.engine.content;

import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.Stats;

/** One row of the GDD §5 roster: identity, level-1 stats, art reference and ability. */
public record HeroDefinition(String id, String name, String title, String kanji, Rank rank, int attack, int health,
                             String sprite, AbilityDefinition ability) {

    public HeroDefinition {
        Ids.require(id, "hero");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("hero " + id + " needs a name");
        }
        if (rank == null) {
            throw new IllegalArgumentException("hero " + id + " needs a rank");
        }
        if (ability == null) {
            throw new IllegalArgumentException("hero " + id + " needs an ability");
        }
        new Stats(attack, health);
    }

    public Stats stats() {
        return new Stats(attack, health);
    }

    /** "Aoi, the Novice Scribe" for a cameo; just "Farmhand" for the others. */
    public String displayName() {
        return title == null ? name : name + ", " + title;
    }
}
