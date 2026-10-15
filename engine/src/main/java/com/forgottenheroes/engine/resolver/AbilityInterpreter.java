package com.forgottenheroes.engine.resolver;

import com.forgottenheroes.engine.content.AbilityDefinition;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.EffectDefinition;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.mutator.UnitMutator;
import com.forgottenheroes.engine.rng.GameRandom;
import java.util.List;
import java.util.Optional;

/**
 * Runs one unit's ability: announces it, builds the moment's context from the unit (its level and
 * attack resolve the amounts), and hands each effect to the {@link EffectApplier}.
 */
public final class AbilityInterpreter {

    private final ContentIndex content;
    private final EffectApplier applier;

    public AbilityInterpreter(ContentIndex content) {
        this.content = content;
        this.applier = new EffectApplier(content);
    }

    /** The ability of a unit, if its hero has one; tokens and test units without a definition have none. */
    public Optional<AbilityDefinition> abilityOf(Unit unit) {
        return content.hasHero(unit.heroId()) ? Optional.of(content.hero(unit.heroId()).ability()) : Optional.empty();
    }

    public void run(Unit unit, AbilityDefinition ability, Board board, Side side, Unit triggering, GameRandom rng, EventLog log,
                    List<PendingTrigger> queue, int summonIndex, List<Unit> deferredSummons) {
        log.append(new AbilityTriggered(0, unit.id(), unit.heroId(), UnitMutator.positionOf(board.band(side), side, unit),
            ability.trigger(), unit.level()));
        EffectContext context = new EffectContext(board, side, unit, null, triggering, unit.level(), unit.attack(), rng, log, queue,
            unit.heroId(), summonIndex, deferredSummons);
        for (EffectDefinition effect : ability.effects()) {
            applier.apply(effect, context);
        }
    }
}
