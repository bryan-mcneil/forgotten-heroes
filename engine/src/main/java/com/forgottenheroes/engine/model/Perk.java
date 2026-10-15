package com.forgottenheroes.engine.model;

/** A status a hero carries, one at a time (GDD §4.8). Some are spent the first time they work. */
public enum Perk {
    FAIRY_CHARM(false),
    IRON_BLADE(false),
    CHAINMAIL(false),
    FLAME_OIL(false),
    WARD(true),
    PHOENIX_FEATHER(true),
    ENCHANTED_BLADE(true),
    AEGIS(true),
    VENOM(false);

    private final boolean consumable;

    Perk(boolean consumable) {
        this.consumable = consumable;
    }

    /** True when the perk is removed after it triggers once. */
    public boolean consumable() {
        return consumable;
    }
}
