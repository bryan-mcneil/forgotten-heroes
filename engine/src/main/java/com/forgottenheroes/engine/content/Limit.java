package com.forgottenheroes.engine.content;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** How often an ability may fire: per turn, per battle, or both; a missing field means no limit. */
public record Limit(Amount perTurn, Amount perBattle) {

    public static final Limit NONE = new Limit(null, null);

    /** Derived, so Jackson must not write it as a field: the JSON holds only perTurn and perBattle. */
    @JsonIgnore
    public boolean isUnlimited() {
        return perTurn == null && perBattle == null;
    }
}
