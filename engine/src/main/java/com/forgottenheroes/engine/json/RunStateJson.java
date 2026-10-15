package com.forgottenheroes.engine.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.BattleSummary;
import com.forgottenheroes.engine.model.Counters;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.Tavern;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;
import java.util.List;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * A {@code RunState} as JSON and back, the identity on every field: what the backend stores per run
 * (Step 28). The mutable model is not touched by Jackson; it goes through plain data records.
 */
public final class RunStateJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .changeDefaultVisibility(visibility -> visibility.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
        .addModule(new EngineJsonModule())
        .build();

    private RunStateJson() { }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(RunState state) {
        return MAPPER.writeValueAsString(RunStateDto.of(state));
    }

    public static RunState fromJson(String json) {
        return MAPPER.readValue(json, RunStateDto.class).toState();
    }

    /** Every field of a run, as data. */
    public record RunStateDto(String runId, long runSeed, int turn, int gold, int hearts, int crowns, List<UnitDto> band,
                              TavernDto tavern, boolean freeRollAvailable, int rollsThisTurn, RunStatus status,
                              BattleSummary lastBattle, long version, int lastEventSeq, int nextUnitNumber) {

        static RunStateDto of(RunState state) {
            return new RunStateDto(state.runId(), state.runSeed(), state.turn(), state.gold(), state.hearts(), state.crowns(),
                state.band().units().stream().map(UnitDto::of).toList(), TavernDto.of(state.tavern()), state.freeRollAvailable(),
                state.rollsThisTurn(), state.status(), state.lastBattle(), state.version(), state.lastEventSeq(),
                state.nextUnitNumber());
        }

        RunState toState() {
            Band restoredBand = new Band();
            for (UnitDto unit : band) {
                restoredBand.add(unit.toUnit());
            }
            return RunState.restore(runId, runSeed, turn, gold, hearts, crowns, restoredBand, tavern.toTavern(), freeRollAvailable,
                rollsThisTurn, status, lastBattle, version, lastEventSeq, nextUnitNumber);
        }
    }

    /** Every field of a unit, as data. */
    public record UnitDto(UnitId id, String heroId, Stats base, Stats permanentBonus, Stats temporaryBonus, int xp, Perk perk,
                          int usesThisTurn, int usesThisBattle, int damageTaken) {

        static UnitDto of(Unit unit) {
            return new UnitDto(unit.id(), unit.heroId(), unit.base(), unit.permanentBonus(), unit.temporaryBonus(), unit.xp().points(),
                unit.perk().orElse(null), unit.counters().usesThisTurn(), unit.counters().usesThisBattle(), unit.damageTaken());
        }

        Unit toUnit() {
            return Unit.restore(id, heroId, base, permanentBonus, temporaryBonus, new Xp(xp), perk,
                Counters.restore(usesThisTurn, usesThisBattle), damageTaken);
        }
    }

    /** The shelves, the permanent buff and the bonus slot, as data. */
    public record TavernDto(List<TavernSlot> heroSlots, List<TavernSlot> relicSlots, Stats permanentBuff, TavernSlot bonusSlot) {

        static TavernDto of(Tavern tavern) {
            return new TavernDto(tavern.heroSlots(), tavern.relicSlots(), tavern.permanentBuff(), tavern.bonusSlot().orElse(null));
        }

        Tavern toTavern() {
            return Tavern.restore(heroSlots, relicSlots, permanentBuff, bonusSlot);
        }
    }
}
