package com.forgottenheroes.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.forgottenheroes.engine.model.ItemKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One tavern action from the client, told apart by the {@code type} field. Every action carries the
 * {@code version} the client last saw; a stale one is answered with 409 (architecture §4.3).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(ActionRequest.Roll.class),
    @JsonSubTypes.Type(ActionRequest.Freeze.class),
    @JsonSubTypes.Type(ActionRequest.Unfreeze.class),
    @JsonSubTypes.Type(ActionRequest.Buy.class),
    @JsonSubTypes.Type(ActionRequest.BuyRelic.class),
    @JsonSubTypes.Type(ActionRequest.Sell.class),
    @JsonSubTypes.Type(ActionRequest.Move.class),
    @JsonSubTypes.Type(ActionRequest.Merge.class)
})
public sealed interface ActionRequest {

    @NotNull
    Long expectedVersion();

    record Roll(Long expectedVersion) implements ActionRequest { }

    record Freeze(@NotNull ItemKind kind, @NotNull @PositiveOrZero Integer slot, Long expectedVersion) implements ActionRequest { }

    record Unfreeze(@NotNull ItemKind kind, @NotNull @PositiveOrZero Integer slot, Long expectedVersion) implements ActionRequest { }

    record Buy(@NotNull @PositiveOrZero Integer slot, @NotNull @PositiveOrZero Integer bandIndex, Long expectedVersion)
        implements ActionRequest { }

    /** {@code bandIndex} is null for relics that pick their own target. */
    record BuyRelic(@NotNull @PositiveOrZero Integer slot, @PositiveOrZero Integer bandIndex, Long expectedVersion)
        implements ActionRequest { }

    record Sell(@NotNull @PositiveOrZero Integer bandIndex, Long expectedVersion) implements ActionRequest { }

    record Move(@NotNull @PositiveOrZero Integer from, @NotNull @PositiveOrZero Integer to, Long expectedVersion)
        implements ActionRequest { }

    record Merge(@NotNull @PositiveOrZero Integer from, @NotNull @PositiveOrZero Integer to, Long expectedVersion)
        implements ActionRequest { }
}
