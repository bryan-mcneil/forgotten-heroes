package com.forgottenheroes.engine.event;

import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.GameEvent.AbilityTriggered;
import com.forgottenheroes.engine.event.GameEvent.BattleEndReason;
import com.forgottenheroes.engine.event.GameEvent.BattleEnded;
import com.forgottenheroes.engine.event.GameEvent.BattleStarted;
import com.forgottenheroes.engine.event.GameEvent.BonusSlotOpened;
import com.forgottenheroes.engine.event.GameEvent.Clash;
import com.forgottenheroes.engine.event.GameEvent.CrownsChanged;
import com.forgottenheroes.engine.event.GameEvent.DamageDealt;
import com.forgottenheroes.engine.event.GameEvent.DamageKind;
import com.forgottenheroes.engine.event.GameEvent.GoldChanged;
import com.forgottenheroes.engine.event.GameEvent.GoldReason;
import com.forgottenheroes.engine.event.GameEvent.HeartsChanged;
import com.forgottenheroes.engine.event.GameEvent.HeartsReason;
import com.forgottenheroes.engine.event.GameEvent.HeroBought;
import com.forgottenheroes.engine.event.GameEvent.HeroSold;
import com.forgottenheroes.engine.event.GameEvent.HeroesMerged;
import com.forgottenheroes.engine.event.GameEvent.HeroesReordered;
import com.forgottenheroes.engine.event.GameEvent.Knockout;
import com.forgottenheroes.engine.event.GameEvent.LevelUp;
import com.forgottenheroes.engine.event.GameEvent.LineShifted;
import com.forgottenheroes.engine.event.GameEvent.MergeSource;
import com.forgottenheroes.engine.event.GameEvent.PerkConsumed;
import com.forgottenheroes.engine.event.GameEvent.PerkGained;
import com.forgottenheroes.engine.event.GameEvent.RelicBought;
import com.forgottenheroes.engine.event.GameEvent.RelicStocked;
import com.forgottenheroes.engine.event.GameEvent.RunEnded;
import com.forgottenheroes.engine.event.GameEvent.RunStarted;
import com.forgottenheroes.engine.event.GameEvent.StatsChanged;
import com.forgottenheroes.engine.event.GameEvent.SummonFailed;
import com.forgottenheroes.engine.event.GameEvent.TavernBuffed;
import com.forgottenheroes.engine.event.GameEvent.TavernItemFrozen;
import com.forgottenheroes.engine.event.GameEvent.TavernItemUnfrozen;
import com.forgottenheroes.engine.event.GameEvent.TavernRolled;
import com.forgottenheroes.engine.event.GameEvent.TurnEnded;
import com.forgottenheroes.engine.event.GameEvent.TurnStarted;
import com.forgottenheroes.engine.event.GameEvent.UnitFell;
import com.forgottenheroes.engine.event.GameEvent.UnitRevived;
import com.forgottenheroes.engine.event.GameEvent.UnitSummoned;
import com.forgottenheroes.engine.event.GameEvent.XpGained;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Side;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.UnitId;
import java.util.List;

/** One of every event type with believable values: the golden file is built from this list. */
final class SampleEvents {

    private static final UnitId U1 = new UnitId("u1");
    private static final UnitId U2 = new UnitId("u2");
    private static final UnitId U3 = new UnitId("u3");
    private static final Position HOME_0 = new Position(Side.HOME, 0);
    private static final Position HOME_1 = new Position(Side.HOME, 1);
    private static final Position AWAY_0 = new Position(Side.AWAY, 0);

    private SampleEvents() { }

    static List<GameEvent> all() {
        EventLog log = new EventLog();
        log.append(new RunStarted(0, "run-42"));
        log.append(new TurnStarted(0, 1, 10, Rank.I, 3, 1));
        log.append(new GoldChanged(0, 0, 10, GoldReason.TURN_START));
        log.append(new TavernRolled(0, true,
            List.of(TavernSlot.hero("aoi", Stats.ZERO), TavernSlot.hero("bard", new Stats(1, 1)).withFrozen(true), TavernSlot.EMPTY),
            List.of(TavernSlot.relic("ration", 3))));
        log.append(new TavernItemFrozen(0, ItemKind.HERO, 1, "bard"));
        log.append(new TavernItemUnfrozen(0, ItemKind.RELIC, 0, "ration"));
        log.append(new TavernBuffed(0, new Stats(1, 1), true));
        log.append(new RelicStocked(0, "ration", 1, 2));
        log.append(new BonusSlotOpened(0, 3, TavernSlot.hero("shield_maiden", Stats.ZERO)));
        log.append(new HeroBought(0, U1, "aoi", 0, 0, new Stats(2, 2), 3));
        log.append(new RelicBought(0, "ration", 0, U1, 3));
        log.append(new RelicBought(0, "royal_decree", 1, null, 3));
        log.append(new HeroSold(0, U2, "merchant", 1, 2, 2));
        log.append(new HeroesReordered(0, 1, 0, List.of(U2, U1)));
        log.append(new HeroesMerged(0, U1, "aoi", MergeSource.BAND, U3, new Stats(2, 2), new Stats(3, 3)));
        log.append(new XpGained(0, U1, "aoi", 1, 2));
        log.append(new LevelUp(0, U1, "aoi", 2));
        log.append(new StatsChanged(0, U1, "aoi", HOME_0, 2, 3, 2, 3, false, "ration"));
        log.append(new PerkGained(0, U1, "aoi", HOME_0, Perk.IRON_BLADE, null));
        log.append(new PerkGained(0, U1, "aoi", HOME_0, Perk.WARD, Perk.IRON_BLADE));
        log.append(new PerkConsumed(0, U1, "aoi", HOME_0, Perk.WARD));
        log.append(new AbilityTriggered(0, U1, "aoi", HOME_0, Trigger.FALL, 2));
        log.append(new TurnEnded(0, 1));
        log.append(new BattleStarted(0, 1, Side.HOME,
            List.of(new UnitView(U1, "aoi", 3, 3, 2, Perk.WARD), new UnitView(U2, "farmhand", 1, 3, 1, null)),
            List.of(new UnitView(U3, "merchant", 4, 1, 1, null))));
        log.append(new Clash(0, 1, U1, U3));
        log.append(new DamageDealt(0, U3, AWAY_0, U1, HOME_0, 4, 3, -1, DamageKind.ATTACK));
        log.append(new DamageDealt(0, U1, HOME_0, U3, AWAY_0, 3, 1, -2, DamageKind.ABILITY));
        log.append(new Knockout(0, U1, "aoi", HOME_0, U3));
        log.append(new UnitFell(0, U1, "aoi", HOME_0));
        log.append(new UnitRevived(0, U1, "aoi", HOME_0, new Stats(1, 1)));
        log.append(new UnitSummoned(0, new UnitId("u4"), "scarecrow", HOME_1, new Stats(1, 1), 1, null, U2));
        log.append(new SummonFailed(0, "wolf", Side.HOME, U2));
        log.append(new LineShifted(0, Side.HOME, List.of(U1, new UnitId("u4"))));
        log.append(new BattleEnded(0, Outcome.WIN, Outcome.WIN, BattleEndReason.ELIMINATION, 3));
        log.append(new CrownsChanged(0, 0, 1));
        log.append(new HeartsChanged(0, 4, 5, HeartsReason.MERCY));
        log.append(new RunEnded(0, RunStatus.WON, 12, 10, 3));
        return log.events();
    }
}
