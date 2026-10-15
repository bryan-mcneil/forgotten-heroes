package com.forgottenheroes.engine.model;

/** Which band a unit belongs to in a battle. HOME is the player on odd turns (GDD §4.5). */
public enum Side {
    HOME, AWAY;

    public Side opposite() {
        return this == HOME ? AWAY : HOME;
    }
}
