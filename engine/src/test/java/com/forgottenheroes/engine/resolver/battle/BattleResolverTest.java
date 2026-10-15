package com.forgottenheroes.engine.resolver.battle;

import static com.forgottenheroes.engine.testing.TestFixtures.band;
import static com.forgottenheroes.engine.testing.TestFixtures.content;
import static com.forgottenheroes.engine.testing.TestFixtures.unit;
import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.BattleEndReason;
import com.forgottenheroes.engine.event.GameEvent.BattleEnded;
import com.forgottenheroes.engine.event.GameEvent.BattleStarted;
import com.forgottenheroes.engine.event.GameEvent.Clash;
import com.forgottenheroes.engine.event.GameEvent.DamageDealt;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.Knockout;
import com.forgottenheroes.engine.event.GameEvent.LineShifted;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Unit;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattleResolverTest {

    private static final BattleResolver RESOLVER = new BattleResolver(content());
    private static final Position HOME_0 = new Position(Side.HOME, 0);
    private static final Position HOME_1 = new Position(Side.HOME, 1);
    private static final Position AWAY_0 = new Position(Side.AWAY, 0);

    private static List<GameEvent> strip(List<GameEvent> events) {
        return events.stream().map(event -> event.withSeq(0)).toList();
    }

    @Test
    void oneAgainstOneHandCalculated() {
        Unit home = unit("a", 2, 2);
        Unit away = unit("b", 1, 2);
        BattleResult result = RESOLVER.resolve(band(home), band(away), 1);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        assertThat(result.playerOutcome()).isEqualTo(Outcome.WIN);
        assertThat(result.clashes()).isEqualTo(1);
        assertThat(strip(result.events().events())).containsExactly(
            new BattleStarted(0, 1, Side.HOME, List.of(com.forgottenheroes.engine.event.UnitView.of(home)),
                List.of(com.forgottenheroes.engine.event.UnitView.of(away))),
            new Clash(0, 1, home.id(), away.id()),
            new DamageDealt(0, home.id(), HOME_0, away.id(), AWAY_0, 2, 2, 0, DamageKind.ATTACK),
            new DamageDealt(0, away.id(), AWAY_0, home.id(), HOME_0, 1, 2, 1, DamageKind.ATTACK),
            new Knockout(0, home.id(), "a", HOME_0, away.id()),
            new UnitFell(0, away.id(), "b", AWAY_0),
            new LineShifted(0, Side.AWAY, List.of()),
            new BattleEnded(0, Outcome.WIN, Outcome.WIN, BattleEndReason.ELIMINATION, 1));
        assertThat(home.damageTaken()).isZero();
    }

    @Test
    void anOpenerCanEndTheBattleBeforeAnyClash() {
        Unit archer = unit("archer", 2, 2);
        Unit aoi = unit("aoi", 2, 2);
        Unit merchant = unit("merchant", 4, 1);
        BattleResult result = RESOLVER.resolve(band(archer, aoi), band(merchant), 7);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        assertThat(result.clashes()).isZero();
        assertThat(result.events().of(AbilityTriggered.class).getFirst().trigger()).isEqualTo(Trigger.START_OF_BATTLE);
        assertThat(result.events().of(DamageDealt.class).getFirst())
            .isEqualTo(new DamageDealt(0, archer.id(), HOME_0, merchant.id(), AWAY_0, 1, 1, 0, DamageKind.ABILITY).withSeq(
                result.events().of(DamageDealt.class).getFirst().seq()));
        assertThat(result.events().of(Clash.class)).isEmpty();
    }

    @Test
    void aoisFallBuffsAnAllyHandCalculated() {
        Unit aoi = unit("aoi", 2, 2);
        Unit farmhand = unit("farmhand", 1, 3);
        Unit merchant = unit("merchant", 4, 1);
        BattleResult result = RESOLVER.resolve(band(aoi, farmhand), band(merchant), 3);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        List<GameEvent> events = strip(result.events().events());
        assertThat(events).containsSubsequence(
            new Clash(0, 1, aoi.id(), merchant.id()),
            new DamageDealt(0, aoi.id(), HOME_0, merchant.id(), AWAY_0, 2, 1, -1, DamageKind.ATTACK),
            new DamageDealt(0, merchant.id(), AWAY_0, aoi.id(), HOME_0, 4, 2, -2, DamageKind.ATTACK),
            new Knockout(0, aoi.id(), "aoi", HOME_0, merchant.id()),
            new Knockout(0, merchant.id(), "merchant", AWAY_0, aoi.id()),
            new UnitFell(0, merchant.id(), "merchant", AWAY_0),
            new UnitFell(0, aoi.id(), "aoi", HOME_0),
            new AbilityTriggered(0, aoi.id(), "aoi", HOME_0, Trigger.FALL, 1),
            new StatsChanged(0, farmhand.id(), "farmhand", HOME_1, 1, 2, 3, 4, true, "aoi"),
            new LineShifted(0, Side.HOME, List.of(farmhand.id())),
            new LineShifted(0, Side.AWAY, List.of()),
            new BattleEnded(0, Outcome.WIN, Outcome.WIN, BattleEndReason.ELIMINATION, 1));
        assertThat(farmhand.stats()).hasToString("1/3");
    }

    @Test
    void twoLastUnitsTradingIsADraw() {
        BattleResult result = RESOLVER.resolve(band(unit("a", 2, 2)), band(unit("b", 2, 2)), 1);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.DRAW);
        assertThat(result.events().of(BattleEnded.class).getFirst().reason()).isEqualTo(BattleEndReason.ELIMINATION);
        assertThat(result.events().of(UnitFell.class)).hasSize(2);
    }

    @Test
    void twoUnkillableUnitsHitTheClashCap() {
        BattleResult result = RESOLVER.resolve(band(unit("a", 0, 1)), band(unit("b", 0, 1)), 1);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.DRAW);
        assertThat(result.reason()).isEqualTo(BattleEndReason.CLASH_CAP);
        assertThat(result.clashes()).isEqualTo(Rules.CLASH_CAP);
        assertThat(result.events().of(Clash.class)).hasSize(Rules.CLASH_CAP);
        assertThat(result.events().of(DamageDealt.class)).allMatch(hit -> hit.amount() == 0);
    }

    @Test
    void theAwayPlayerSeesTheFlippedOutcome() {
        BattleResult result = RESOLVER.resolve(band(unit("a", 5, 5)), band(unit("b", 1, 1)), 1, 4, Side.AWAY);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        assertThat(result.playerOutcome()).isEqualTo(Outcome.LOSS);
        BattleEnded ended = result.events().of(BattleEnded.class).getFirst();
        assertThat(ended.playerOutcome()).isEqualTo(Outcome.LOSS);
        assertThat(result.events().of(BattleStarted.class).getFirst().playerSide()).isEqualTo(Side.AWAY);
        assertThat(result.events().of(BattleStarted.class).getFirst().turn()).isEqualTo(4);
    }

    @Test
    void shieldMaidenGrowsWhenHurt() {
        Unit maiden = unit("shield_maiden", 2, 5);
        Unit brute = unit("b", 2, 7);
        BattleResult result = RESOLVER.resolve(band(maiden), band(brute), 1);
        assertThat(result.homeOutcome()).isEqualTo(Outcome.WIN);
        List<DamageDealt> hits = result.events().of(DamageDealt.class).stream().filter(hit -> hit.sourceUnitId().equals(maiden.id())).toList();
        assertThat(hits).extracting(DamageDealt::amount).containsExactly(2, 6);
        assertThat(result.events().of(AbilityTriggered.class)).extracting(AbilityTriggered::trigger).containsExactly(Trigger.HURT, Trigger.HURT);
    }

    @Test
    void theBandsHandedInAreNeverChanged() {
        Unit a = unit("a", 2, 2);
        Band home = band(a);
        Band before = home.copy();
        RESOLVER.resolve(home, band(unit("b", 5, 5)), 1);
        assertThat(home).isEqualTo(before);
        assertThat(a.isAlive()).isTrue();
    }

    @Test
    void sameBandsAndSeedGiveTheSameJson() {
        Band home = band(unit("archer", 2, 2), unit("aoi", 2, 2), unit("shield_maiden", 2, 5));
        Band away = band(unit("merchant", 4, 1), unit("bard", 2, 1), unit("archer", 2, 2));
        String first = EventJson.toJson(RESOLVER.resolve(home, away, 99).events().events());
        String second = EventJson.toJson(RESOLVER.resolve(home, away, 99).events().events());
        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(EventJson.toJson(RESOLVER.resolve(home, away, 100).events().events()));
    }

    @Test
    void printsStory() {
        Band home = band(unit("archer", 2, 2), unit("aoi", 2, 2), unit("farmhand", 1, 3));
        Band away = band(unit("merchant", 4, 1), unit("shield_maiden", 2, 5));
        BattleResult result = RESOLVER.resolve(home, away, 42);
        System.out.println(result.events().story());
        assertThat(result.events().story()).contains("BattleStarted").contains("Clash").contains("BattleEnded");
        assertThat(result.events().events().getLast()).isInstanceOf(BattleEnded.class);
    }
}
