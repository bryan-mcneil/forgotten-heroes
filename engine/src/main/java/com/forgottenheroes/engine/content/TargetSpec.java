package com.forgottenheroes.engine.content;

/** Which units an effect reaches: a selector plus how many (default 1). */
public record TargetSpec(TargetSelect select, Amount count) {

    public TargetSpec {
        if (select == null) {
            throw new IllegalArgumentException("target needs a select");
        }
        if (count == null) {
            count = Amount.flat(1);
        }
    }

    public static TargetSpec of(TargetSelect select) {
        return new TargetSpec(select, null);
    }
}
