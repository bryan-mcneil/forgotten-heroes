package com.forgottenheroes.engine.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.model.Rank;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentIndexTest {

    private static HeroDefinition hero(String id, Rank rank, EffectDefinition effect) {
        AbilityDefinition ability = new AbilityDefinition(Trigger.FALL, "text", List.of(effect), null);
        return new HeroDefinition(id, id, null, null, rank, 1, 1, "sprite", ability);
    }

    private static final EffectDefinition GOLD = new EffectDefinition.GainGold(Amount.flat(1));
    private static final TokenDefinition WOLF = new TokenDefinition("wolf", "Wolf", 2, 2, "mon/wolf1");
    private static final RelicDefinition RATION = new RelicDefinition("ration", "Ration", Rank.I, 3, "text", "icon",
        List.of(new EffectDefinition.ModifyStats(TargetSpec.of(TargetSelect.CHOSEN), Amount.flat(1), Amount.flat(1), false)));

    @Test
    void ranksFilterTheTavernPool() {
        ContentIndex index = new ContentIndex(
            List.of(hero("a", Rank.I, GOLD), hero("b", Rank.II, GOLD), hero("c", Rank.IV, GOLD)),
            List.of(),
            List.of(RATION, new RelicDefinition("elixir", "Elixir", Rank.IV, 3, "text", "icon", RATION.effects())));
        assertThat(index.heroesOfRankUpTo(Rank.I)).extracting(HeroDefinition::id).containsExactly("a");
        assertThat(index.heroesOfRankUpTo(Rank.III)).extracting(HeroDefinition::id).containsExactly("a", "b");
        assertThat(index.heroesOfRankUpTo(Rank.VI)).extracting(HeroDefinition::id).containsExactly("a", "b", "c");
        assertThat(index.relicsOfRankUpTo(Rank.III)).extracting(RelicDefinition::id).containsExactly("ration");
        assertThat(index.heroesOfRank(Rank.II)).extracting(HeroDefinition::id).containsExactly("b");
    }

    @Test
    void unknownIdsThrowAReadableError() {
        ContentIndex index = new ContentIndex(List.of(hero("a", Rank.I, GOLD)), List.of(WOLF), List.of(RATION));
        assertThatThrownBy(() -> index.hero("zzz")).isInstanceOf(ContentException.class).hasMessageContaining("zzz");
        assertThatThrownBy(() -> index.token("zzz")).isInstanceOf(ContentException.class).hasMessageContaining("zzz");
        assertThatThrownBy(() -> index.relic("zzz")).isInstanceOf(ContentException.class).hasMessageContaining("zzz");
        assertThat(index.hasHero("a")).isTrue();
        assertThat(index.hasRelic("ration")).isTrue();
        assertThat(index.token("wolf")).isEqualTo(WOLF);
    }

    @Test
    void duplicateIdsAreRejected() {
        assertThatThrownBy(() -> new ContentIndex(List.of(hero("a", Rank.I, GOLD), hero("a", Rank.I, GOLD)), List.of(), List.of()))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("duplicate hero id 'a'");
    }

    @Test
    void summonOfAnUnknownTokenIsRejected() {
        EffectDefinition summon = new EffectDefinition.Summon("ghost", null, null, null, null, null, null);
        assertThatThrownBy(() -> new ContentIndex(List.of(hero("a", Rank.I, summon)), List.of(WOLF), List.of()))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("hero a summons unknown token 'ghost'");
    }

    @Test
    void stockingAnUnknownRelicIsRejected() {
        EffectDefinition stock = new EffectDefinition.StockRelic("elixir", Amount.flat(2));
        assertThatThrownBy(() -> new ContentIndex(List.of(hero("a", Rank.I, stock)), List.of(), List.of(RATION)))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("stocks unknown relic 'elixir'");
    }

    @Test
    void summaryCountsPerRank() {
        ContentIndex index = new ContentIndex(List.of(hero("a", Rank.I, GOLD), hero("b", Rank.I, GOLD)), List.of(WOLF), List.of(RATION));
        assertThat(index.summary()).isEqualTo("heroes 2 (I=2 II=0 III=0 IV=0 V=0 VI=0), tokens 1, relics 1 (I=1 II=0 III=0 IV=0 V=0 VI=0)");
    }

    @Test
    void badDefinitionsAreRejectedBeforeIndexing() {
        assertThatThrownBy(() -> new TokenDefinition("Wolf", "Wolf", 1, 1, "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HeroDefinition("a", "A", null, null, Rank.I, 1, 1, "s", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RelicDefinition("r", "R", Rank.I, 3, "t", "s", List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AbilityDefinition(Trigger.BUY, "t", List.of(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Condition(1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EffectDefinition.ModifyStats(TargetSpec.of(TargetSelect.SELF), null, null, false))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
