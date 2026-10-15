package com.forgottenheroes.engine.model;

/** A slot in a band: side plus index, where 0 is the front (fights first). */
public record Position(Side side, int index) {

    public Position {
        if (index < 0 || index >= Rules.BAND_SIZE) {
            throw new IllegalArgumentException("index must be 0.." + (Rules.BAND_SIZE - 1) + ", got " + index);
        }
    }

    /** Reads "home:0" back into a position, the inverse of toString. */
    public static Position parse(String text) {
        int colon = text == null ? -1 : text.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("position must look like home:0, got '" + text + "'");
        }
        Side side = Side.valueOf(text.substring(0, colon).toUpperCase());
        return new Position(side, Integer.parseInt(text.substring(colon + 1)));
    }

    public boolean isFront() {
        return index == 0;
    }

    @Override
    public String toString() {
        return side.name().toLowerCase() + ":" + index;
    }
}
