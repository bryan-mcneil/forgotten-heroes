package com.forgottenheroes.engine.model;

/** How a battle ended for one side. A draw costs nothing and awards nothing (GDD §4.1). */
public enum Outcome {
    WIN, LOSS, DRAW;

    /** The same battle seen from the other band. */
    public Outcome flipped() {
        return switch (this) {
            case WIN -> LOSS;
            case LOSS -> WIN;
            case DRAW -> DRAW;
        };
    }
}
