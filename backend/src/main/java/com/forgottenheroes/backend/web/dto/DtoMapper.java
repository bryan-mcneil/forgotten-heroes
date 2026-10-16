package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.model.BattleSummary;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import java.util.List;

/** Engine state → wire shapes, and client requests → engine actions. Copies values; applies no rule. */
public final class DtoMapper {

    private DtoMapper() { }

    public static RunStateDto toDto(RunState state) {
        return new RunStateDto(
            state.runId(),
            state.turn(),
            state.gold(),
            state.hearts(),
            state.crowns(),
            state.status(),
            state.freeRollAvailable(),
            new BandDto(state.band().units().stream().map(DtoMapper::toDto).toList()),
            toDto(state.tavern()),
            toDto(state.lastBattle()),
            state.version(),
            state.lastEventSeq());
    }

    static UnitDto toDto(Unit unit) {
        return new UnitDto(unit.id().value(), unit.heroId(), unit.attack(), unit.health(), unit.level(), unit.xp().points(),
            unit.perk().orElse(null));
    }

    static TavernDto toDto(Tavern tavern) {
        return new TavernDto(toDtos(tavern.heroSlots()), toDtos(tavern.relicSlots()), tavern.bonusSlot().map(DtoMapper::toDto).orElse(null));
    }

    private static List<TavernSlotDto> toDtos(List<TavernSlot> slots) {
        return slots.stream().map(DtoMapper::toDto).toList();
    }

    static TavernSlotDto toDto(TavernSlot slot) {
        return new TavernSlotDto(slot.itemId(), slot.price(), slot.bonus().attack(), slot.bonus().health(), slot.frozen());
    }

    static BattleSummaryDto toDto(BattleSummary summary) {
        return summary == null ? null : new BattleSummaryDto(summary.turn(), summary.outcome(), summary.clashes());
    }

    /** The client's request as the engine's action; the sealed switch fails to compile if a shape is forgotten. */
    public static Action toAction(ActionRequest request) {
        return switch (request) {
            case ActionRequest.Roll r -> new Action.Roll();
            case ActionRequest.Freeze f -> new Action.Freeze(f.kind(), f.slot());
            case ActionRequest.Unfreeze u -> new Action.Unfreeze(u.kind(), u.slot());
            case ActionRequest.Buy b -> new Action.Buy(b.slot(), b.bandIndex());
            case ActionRequest.BuyRelic b -> new Action.BuyRelic(b.slot(), b.bandIndex());
            case ActionRequest.Sell s -> new Action.Sell(s.bandIndex());
            case ActionRequest.Move m -> new Action.Move(m.from(), m.to());
            case ActionRequest.Merge m -> new Action.Merge(m.from(), m.to());
        };
    }
}
