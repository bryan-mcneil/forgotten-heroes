package com.forgottenheroes.engine.content;

import com.forgottenheroes.engine.model.Rank;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every hero, token and relic by id, in file order. Built once at startup; immutable afterwards.
 * The constructor cross-checks the content: unique ids, and every summon or stock effect points at
 * a token or relic that exists.
 */
public final class ContentIndex {

    private final Map<String, HeroDefinition> heroes;
    private final Map<String, TokenDefinition> tokens;
    private final Map<String, RelicDefinition> relics;

    public ContentIndex(List<HeroDefinition> heroes, List<TokenDefinition> tokens, List<RelicDefinition> relics) {
        this.heroes = index(heroes, HeroDefinition::id, "hero");
        this.tokens = index(tokens, TokenDefinition::id, "token");
        this.relics = index(relics, RelicDefinition::id, "relic");
        for (HeroDefinition hero : heroes) {
            checkReferences("hero " + hero.id(), hero.ability().effects());
        }
        for (RelicDefinition relic : relics) {
            checkReferences("relic " + relic.id(), relic.effects());
        }
    }

    public HeroDefinition hero(String id) {
        HeroDefinition hero = heroes.get(id);
        if (hero == null) {
            throw new ContentException("unknown hero id '" + id + "'");
        }
        return hero;
    }

    public TokenDefinition token(String id) {
        TokenDefinition token = tokens.get(id);
        if (token == null) {
            throw new ContentException("unknown token id '" + id + "'");
        }
        return token;
    }

    public RelicDefinition relic(String id) {
        RelicDefinition relic = relics.get(id);
        if (relic == null) {
            throw new ContentException("unknown relic id '" + id + "'");
        }
        return relic;
    }

    public boolean hasHero(String id) {
        return heroes.containsKey(id);
    }

    public boolean hasToken(String id) {
        return tokens.containsKey(id);
    }

    public boolean hasRelic(String id) {
        return relics.containsKey(id);
    }

    public List<HeroDefinition> heroes() {
        return List.copyOf(heroes.values());
    }

    public List<TokenDefinition> tokens() {
        return List.copyOf(tokens.values());
    }

    public List<RelicDefinition> relics() {
        return List.copyOf(relics.values());
    }

    public List<HeroDefinition> heroesOfRank(Rank rank) {
        return heroes.values().stream().filter(hero -> hero.rank() == rank).toList();
    }

    /** What the tavern can draw from on a turn where this rank is the highest available (GDD §4.3). */
    public List<HeroDefinition> heroesOfRankUpTo(Rank rank) {
        return heroes.values().stream().filter(hero -> hero.rank().compareTo(rank) <= 0).toList();
    }

    public List<RelicDefinition> relicsOfRankUpTo(Rank rank) {
        return relics.values().stream().filter(relic -> relic.rank().compareTo(rank) <= 0).toList();
    }

    /** One line for logs: counts per rank, so a content step can prove what it added. */
    public String summary() {
        StringBuilder text = new StringBuilder("heroes " + heroes.size() + " (");
        appendPerRank(text, heroes.values().stream().map(HeroDefinition::rank).toList());
        text.append("), tokens ").append(tokens.size()).append(", relics ").append(relics.size()).append(" (");
        appendPerRank(text, relics.values().stream().map(RelicDefinition::rank).toList());
        return text.append(")").toString();
    }

    private static void appendPerRank(StringBuilder text, List<Rank> ranks) {
        List<String> parts = new ArrayList<>();
        for (Rank rank : Rank.values()) {
            parts.add(rank + "=" + ranks.stream().filter(r -> r == rank).count());
        }
        text.append(String.join(" ", parts));
    }

    private static <T> Map<String, T> index(List<T> items, java.util.function.Function<T, String> id, String what) {
        Map<String, T> map = new LinkedHashMap<>();
        for (T item : items) {
            if (map.put(id.apply(item), item) != null) {
                throw new ContentException("duplicate " + what + " id '" + id.apply(item) + "'");
            }
        }
        return map;
    }

    private void checkReferences(String owner, List<EffectDefinition> effects) {
        for (EffectDefinition effect : effects) {
            switch (effect) {
                case EffectDefinition.Summon summon -> {
                    if (!tokens.containsKey(summon.tokenId())) {
                        throw new ContentException(owner + " summons unknown token '" + summon.tokenId() + "'");
                    }
                }
                case EffectDefinition.StockRelic stock -> {
                    if (!relics.containsKey(stock.relicId())) {
                        throw new ContentException(owner + " stocks unknown relic '" + stock.relicId() + "'");
                    }
                }
                default -> { }
            }
        }
    }
}
