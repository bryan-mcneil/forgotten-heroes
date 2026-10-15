package com.forgottenheroes.engine.resolver;

import static com.forgottenheroes.engine.testing.TestFixtures.band;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.content.Amount;
import com.forgottenheroes.engine.content.TargetSelect;
import com.forgottenheroes.engine.content.TargetSpec;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.rng.GameRandom;
import com.forgottenheroes.engine.testing.TwoSidedBoard;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TargetingTest {

    private final Unit h0 = unit("h0", 1, 1);
    private final Unit h1 = unit("h1", 2, 2);
    private final Unit h2 = unit("h2", 3, 3);
    private final Unit h3 = unit("h3", 4, 4, 2);
    private final Unit a0 = unit("a0", 5, 9);
    private final Unit a1 = unit("a1", 6, 2);
    private final Unit a2 = unit("a2", 7, 7);
    private final Band home = band(h0, h1, h2, h3);
    private final Band away = band(a0, a1, a2);

    private EffectContext from(Unit self) {
        return new EffectContext(new TwoSidedBoard(home, away), Side.HOME, self, null, null, 1, self == null ? 0 : self.attack(),
            new GameRandom(5), new EventLog(), new ArrayList<>(), "test", 0, null);
    }

    private List<Unit> select(TargetSelect select, int count, Unit self) {
        return Targeting.select(new TargetSpec(select, Amount.flat(count)), from(self));
    }

    @Test
    void alliesNeverIncludeSelfAndEnemiesAreTheOtherBand() {
        assertThat(select(TargetSelect.ALL_ALLIES, 1, h1)).containsExactly(h0, h2, h3);
        assertThat(select(TargetSelect.ALL_ENEMIES, 1, h1)).containsExactly(a0, a1, a2);
        assertThat(select(TargetSelect.ALL_UNITS, 1, h1)).containsExactly(h0, h1, h2, h3, a0, a1, a2);
        assertThat(select(TargetSelect.SELF, 1, h1)).containsExactly(h1);
        assertThat(select(TargetSelect.SELF, 1, null)).isEmpty();
    }

    @Test
    void positionsAreRelativeToSelf() {
        assertThat(select(TargetSelect.NEAREST_ALLIES_BEHIND, 2, h1)).containsExactly(h2, h3);
        assertThat(select(TargetSelect.NEAREST_ALLIES_BEHIND, 5, h2)).containsExactly(h3);
        assertThat(select(TargetSelect.NEAREST_ALLIES_AHEAD, 1, h2)).containsExactly(h1);
        assertThat(select(TargetSelect.ADJACENT, 1, h1)).containsExactly(h0, h2);
        assertThat(select(TargetSelect.ADJACENT, 1, h0)).containsExactly(h1);
        assertThat(select(TargetSelect.ADJACENT, 1, h3)).containsExactly(h2);
        assertThat(select(TargetSelect.FRONT_ALLY, 1, h0)).containsExactly(h1);
        assertThat(select(TargetSelect.FRONT_ALLY, 1, h2)).containsExactly(h0);
    }

    @Test
    void enemyPicksAreDeterministic() {
        assertThat(select(TargetSelect.FIRST_ENEMY, 1, h0)).containsExactly(a0);
        assertThat(select(TargetSelect.LAST_ENEMY, 1, h0)).containsExactly(a2);
        assertThat(select(TargetSelect.LOWEST_HEALTH_ENEMY, 1, h0)).containsExactly(a1);
        assertThat(select(TargetSelect.HIGHEST_HEALTH_ENEMY, 1, h0)).containsExactly(a0);
        assertThat(select(TargetSelect.RANDOM_ENEMY, 2, h0)).hasSize(2).doesNotHaveDuplicates();
        assertThat(select(TargetSelect.RANDOM_ENEMY, 2, h0)).isEqualTo(select(TargetSelect.RANDOM_ENEMY, 2, h0));
        assertThat(select(TargetSelect.LEVEL_2_PLUS_ALLIES, 2, h0)).containsExactly(h3);
    }

    @Test
    void deadUnitsAreSkippedAndTheTriggeringUnitMustBeAlive() {
        a0.takeDamage(9);
        h2.takeDamage(3);
        assertThat(select(TargetSelect.FIRST_ENEMY, 1, h0)).containsExactly(a1);
        assertThat(select(TargetSelect.NEAREST_ALLIES_BEHIND, 1, h1)).containsExactly(h3);
        EffectContext context = new EffectContext(new TwoSidedBoard(home, away), Side.HOME, h0, null, h2, 1, 1, new GameRandom(1),
            new EventLog(), new ArrayList<>(), "test", 0, null);
        assertThat(Targeting.select(TargetSpec.of(TargetSelect.TRIGGERING_UNIT), context)).isEmpty();
    }

    @Test
    void enemiesAreEmptyInTheTavernAndTavernHeroesIsNotAUnitSelector() {
        EffectContext tavern = EffectContext.forRelic(com.forgottenheroes.engine.testing.TestFixtures.newRun(1), null,
            new GameRandom(1), new EventLog(), new ArrayList<>(), "x");
        assertThat(Targeting.select(TargetSpec.of(TargetSelect.ALL_ENEMIES), tavern)).isEmpty();
        assertThat(Targeting.select(TargetSpec.of(TargetSelect.NEAREST_ALLIES_BEHIND), tavern)).isEmpty();
        assertThatThrownBy(() -> Targeting.select(TargetSpec.of(TargetSelect.TAVERN_HEROES), tavern))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
