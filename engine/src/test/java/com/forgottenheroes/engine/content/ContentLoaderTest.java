package com.forgottenheroes.engine.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.Rank;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentLoaderTest {

    private static final ContentIndex CONTENT = ContentLoader.load();

    @Test
    void theRealContentLoadsAndPrintsItsSummary() {
        System.out.println("content: " + CONTENT.summary());
        assertThat(CONTENT.summary()).startsWith("heroes 12 (I=6 II=6").endsWith("relics 16 (I=2 II=3 III=2 IV=2 V=3 VI=4)");
    }

    @Test
    void everyRankIHeroOfTheGddIsThere() {
        assertThat(CONTENT.heroesOfRank(Rank.I)).extracting(HeroDefinition::id)
            .containsExactly("aoi", "farmhand", "merchant", "bard", "archer", "hotaru");
        assertThat(CONTENT.hero("merchant").stats()).hasToString("4/1");
        assertThat(CONTENT.hero("aoi").displayName()).isEqualTo("Aoi, the Novice Scribe");
        assertThat(CONTENT.hero("farmhand").displayName()).isEqualTo("Farmhand");
    }

    @Test
    void idsAreUnique() {
        List<String> ids = CONTENT.heroes().stream().map(HeroDefinition::id).toList();
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(CONTENT.relics().stream().map(RelicDefinition::id).toList()).doesNotHaveDuplicates();
    }

    @Test
    void everySummonPointsAtAnExistingToken() {
        for (HeroDefinition hero : CONTENT.heroes()) {
            for (EffectDefinition effect : hero.ability().effects()) {
                if (effect instanceof EffectDefinition.Summon summon) {
                    assertThat(CONTENT.hasToken(summon.tokenId())).as(hero.id()).isTrue();
                }
            }
        }
    }

    @Test
    void theJsonTypeFieldPicksTheEffectRecord() {
        AbilityDefinition aoi = CONTENT.hero("aoi").ability();
        assertThat(aoi.trigger()).isEqualTo(Trigger.FALL);
        assertThat(aoi.effects()).hasSize(1);
        assertThat(aoi.effects().getFirst()).isInstanceOf(EffectDefinition.ModifyStats.class);
        EffectDefinition.ModifyStats buff = (EffectDefinition.ModifyStats) aoi.effects().getFirst();
        assertThat(buff.target().select()).isEqualTo(TargetSelect.RANDOM_ALLY);
        assertThat(buff.attack().resolve(2, 0)).isEqualTo(2);

        assertThat(CONTENT.hero("farmhand").ability().effects().getFirst())
            .isInstanceOf(EffectDefinition.Summon.class);
        assertThat(CONTENT.hero("merchant").ability().effects().getFirst())
            .isInstanceOf(EffectDefinition.GainGold.class);
    }

    @Test
    void defaultsFillTheOptionalFields() {
        EffectDefinition.Summon scarecrow = (EffectDefinition.Summon) CONTENT.hero("farmhand").ability().effects().getFirst();
        assertThat(scarecrow.count()).isEqualTo(Amount.flat(1));
        assertThat(scarecrow.side()).isEqualTo(EffectDefinition.SummonSide.ALLY);
        assertThat(scarecrow.position()).isEqualTo(EffectDefinition.SummonPosition.HERE);
        assertThat(CONTENT.hero("aoi").ability().limit()).isEqualTo(Limit.NONE);
        assertThat(CONTENT.hero("aoi").ability().effects().getFirst().target().count()).isEqualTo(Amount.flat(1));
    }

    @Test
    void relicsKnowTheirKind() {
        assertThat(CONTENT.relic("ration").isPerk()).isFalse();
        assertThat(CONTENT.relic("ration").needsChosenTarget()).isTrue();
        assertThat(CONTENT.relic("iron_blade").isPerk()).isTrue();
        assertThat(CONTENT.relic("poison_vial").price()).isEqualTo(1);
        EffectDefinition.GivePerk perk = (EffectDefinition.GivePerk) CONTENT.relic("fairy_charm").effects().getFirst();
        assertThat(perk.perk()).isEqualTo(Perk.FAIRY_CHARM);
    }

    @Test
    void aTypoInTheJsonFailsWithThePathToIt() {
        assertThatThrownBy(() -> ContentLoader.load("content-broken"))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("/heroes/0/ability")
            .hasMessageContaining("triger")
            .hasMessageContaining("attack");
    }

    @Test
    void invalidJsonTextIsReportedWithTheFileName() {
        assertThatThrownBy(() -> ContentLoader.fromJson("[{", "[]", "[]"))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("heroes.json");
    }

    @Test
    void aMissingFolderIsReported() {
        assertThatThrownBy(() -> ContentLoader.load("no-such-folder"))
            .isInstanceOf(ContentException.class)
            .hasMessageContaining("no-such-folder/heroes.json");
    }

    @Test
    void rankRoundTripsAsANumber() {
        String json = ContentLoader.mapper().writeValueAsString(CONTENT.hero("aoi"));
        assertThat(json).contains("\"rank\":1");
        assertThat(ContentLoader.mapper().readValue(json, HeroDefinition.class)).isEqualTo(CONTENT.hero("aoi"));
    }
}
