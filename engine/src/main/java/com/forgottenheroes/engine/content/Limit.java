package com.forgottenheroes.engine.content;

/** How often an ability may fire: per turn, per battle, or both; a missing field means no limit. */
public record Limit(Amount perTurn, Amount perBattle) {

    public static final Limit NONE = new Limit(null, null);

    public boolean isUnlimited() {
        return perTurn == null && perBattle == null;
    }
}
