package com.forgottenheroes.engine.model;

/** A slot in a band: side plus index, where 0 is the front (fights first). */
public record Position(Side side, int index) {

    public Position {
        if (index < 0 || index >= Rules.BAND_SIZE) {
            throw new IllegalArgumentException("index must be 0.." + (Rules.BAND_SIZE - 1) + ", got " + index);
        }
    }

    public boolean isFront() {
        return index == 0;
    }

    @Override
    public String toString() {
        return side.name().toLowerCase() + ":" + index;
    }
}
