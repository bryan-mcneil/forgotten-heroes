package com.forgottenheroes.engine.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.Trigger;
import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.event.EventLog;
import com.forgottenheroes.engine.event.GameEvent;
import com.forgottenheroes.engine.model.Band;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.Perk;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.Stats;
import com.forgottenheroes.engine.model.TavernSlot;
import com.forgottenheroes.engine.model.Unit;
import com.forgottenheroes.engine.model.UnitId;
import com.forgottenheroes.engine.model.Xp;
import com.forgottenheroes.engine.resolver.TavernResolver;
import com.forgottenheroes.engine.resolver.battle.BattleResolver;
import com.forgottenheroes.engine.resolver.battle.BattleResult;
import com.forgottenheroes.engine.testing.TestFixtures;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Runs one scenario file (quality strategy §2). A scenario with {@code home} and {@code away} is a
 * battle; one with {@code band} and {@code actions} is a tavern sequence. Expected events are matched
 * as an in-order subsequence of the actual events, by the fields the file names: {@code unit},
 * {@code target} and {@code source} are positions, {@code attack}/{@code health}/{@code gold}/{@code xp}
 * read "before→after", {@code token} and {@code hero} are hero ids, {@code stats} is "attack/health".
 */
final class ScenarioRunner {

    private static final ContentIndex CONTENT = TestFixtures.content();
    private static final BattleResolver BATTLE = new BattleResolver(CONTENT);
    private static final TavernResolver TAVERN = new TavernResolver(CONTENT);

    private ScenarioRunner() { }

    static void run(JsonNode scenario) {
        if (scenario.has("away")) {
            runBattle(scenario);
        } else {
            runTavern(scenario);
        }
    }

    private static void runBattle(JsonNode scenario) {
        Band home = band(scenario.get("home"), "h");
        Band away = band(scenario.get("away"), "a");
        long seed = scenario.has("seed") ? scenario.get("seed").asLong() : 1;
        BattleResult result = BATTLE.resolve(home, away, seed);
        expectEvents(scenario, result.events().events());
        if (scenario.has("expectResult")) {
            assertThat(result.homeOutcome()).as("result").isEqualTo(Outcome.valueOf(scenario.get("expectResult").asString()));
        }
    }

    private static void runTavern(JsonNode scenario) {
        long seed = scenario.has("seed") ? scenario.get("seed").asLong() : 1;
        int turn = scenario.has("turn") ? scenario.get("turn").asInt() : 3;
        RunState state = TestFixtures.runAtTurn(seed, turn);
        if (scenario.has("gold")) {
            state.setGold(scenario.get("gold").asInt());
        }
        if (scenario.has("band")) {
            for (JsonNode spec : scenario.get("band")) {
                state.band().add(unit(spec, state.nextUnitId()));
            }
        }
        JsonNode tavern = scenario.get("tavern");
        if (tavern != null) {
            if (tavern.has("heroes")) {
                int i = 0;
                for (JsonNode id : tavern.get("heroes")) {
                    state.tavern().setHeroSlot(i++, TavernSlot.hero(id.asString(), state.tavern().permanentBuff()));
                }
            }
            if (tavern.has("relics")) {
                int i = 0;
                for (JsonNode id : tavern.get("relics")) {
                    if (i >= state.tavern().relicSlots().size()) {
                        state.tavern().addRelicSlot(TavernSlot.relic(id.asString(), CONTENT.relic(id.asString()).price()));
                    } else {
                        state.tavern().setRelicSlot(i, TavernSlot.relic(id.asString(), CONTENT.relic(id.asString()).price()));
                    }
                    i++;
                }
            }
        }
        List<GameEvent> events = new ArrayList<>();
        for (JsonNode actionNode : scenario.get("actions")) {
            if ("EndOfTurn".equals(actionNode.get("type").asString())) {
                EventLog log = TAVERN.fire(state, Trigger.END_OF_TURN);
                events.addAll(log.events());
            } else {
                Action action = EventJson.mapper().treeToValue(actionNode, Action.class);
                events.addAll(TAVERN.apply(state, action).events().events());
            }
        }
        state.assertInvariants();
        expectEvents(scenario, events);
        expectState(scenario.get("expectState"), state);
    }

    private static Band band(JsonNode specs, String prefix) {
        Band band = new Band();
        int n = 1;
        for (JsonNode spec : specs) {
            band.add(unit(spec, new UnitId(prefix + n++)));
        }
        return band;
    }

    /** A unit from {@code {"hero": id, "attack"?, "health"?, "level"?, "perk"?}}; stats default to the definition's. */
    private static Unit unit(JsonNode spec, UnitId id) {
        String heroId = spec.get("hero").asString();
        Stats base = CONTENT.hasHero(heroId) ? CONTENT.hero(heroId).stats() : CONTENT.token(heroId).stats();
        if (spec.has("attack")) {
            base = base.withAttack(spec.get("attack").asInt());
        }
        if (spec.has("health")) {
            base = base.withHealth(spec.get("health").asInt());
        }
        Unit unit = new Unit(id, heroId, base);
        if (spec.has("level")) {
            int level = spec.get("level").asInt();
            unit.gainXp(level == 1 ? 0 : level == 2 ? Xp.LEVEL2_AT : Xp.LEVEL3_AT);
        }
        if (spec.has("xp")) {
            unit.gainXp(spec.get("xp").asInt());
        }
        if (spec.has("perk")) {
            unit.setPerk(Perk.valueOf(spec.get("perk").asString()));
        }
        return unit;
    }

    private static void expectEvents(JsonNode scenario, List<GameEvent> events) {
        List<JsonNode> actual = events.stream().map(event -> (JsonNode) EventJson.mapper().valueToTree(event)).toList();
        if (scenario.has("expectEvents")) {
            int cursor = 0;
            for (JsonNode expected : scenario.get("expectEvents")) {
                int found = -1;
                for (int i = cursor; i < actual.size(); i++) {
                    if (matches(expected, actual.get(i))) {
                        found = i;
                        break;
                    }
                }
                if (found < 0) {
                    fail("expected event " + expected + " not found after event #" + cursor + " in:\n" + story(events));
                }
                cursor = found + 1;
            }
        }
        if (scenario.has("expectCounts")) {
            Map<String, Integer> counts = new HashMap<>();
            for (JsonNode node : actual) {
                counts.merge(node.get("type").asString(), 1, Integer::sum);
            }
            Iterator<Map.Entry<String, JsonNode>> fields = scenario.get("expectCounts").properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                assertThat(counts.getOrDefault(field.getKey(), 0)).as("count of " + field.getKey() + " in:\n" + story(events))
                    .isEqualTo(field.getValue().asInt());
            }
        }
    }

    private static boolean matches(JsonNode expected, JsonNode actual) {
        Iterator<Map.Entry<String, JsonNode>> fields = expected.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String key = field.getKey();
            String want = field.getValue().asString();
            String got = switch (key) {
                case "unit" -> text(actual, "position");
                case "target" -> text(actual, "targetPosition");
                case "source" -> text(actual, "sourcePosition");
                case "token", "hero" -> text(actual, "heroId");
                case "stats" -> actual.has("stats") ? actual.get("stats").get("attack").asString() + "/" + actual.get("stats").get("health").asString() : null;
                case "attack", "health" -> arrow(actual, key + "Before", key + "After");
                case "gold", "hearts", "crowns", "xp" -> arrow(actual, "before", "after");
                default -> text(actual, key);
            };
            if (got == null || !got.equals(want)) {
                return false;
            }
        }
        return true;
    }

    private static String text(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asString() : null;
    }

    private static String arrow(JsonNode node, String before, String after) {
        return node.has(before) && node.has(after) ? node.get(before).asString() + "→" + node.get(after).asString() : null;
    }

    private static void expectState(JsonNode expected, RunState state) {
        if (expected == null) {
            return;
        }
        if (expected.has("gold")) {
            assertThat(state.gold()).as("gold").isEqualTo(expected.get("gold").asInt());
        }
        if (expected.has("hearts")) {
            assertThat(state.hearts()).as("hearts").isEqualTo(expected.get("hearts").asInt());
        }
        if (expected.has("bonusSlot")) {
            assertThat(state.tavern().bonusSlot().isPresent()).as("bonus slot").isEqualTo(expected.get("bonusSlot").asBoolean());
        }
        if (expected.has("tavernBuff")) {
            assertThat(state.tavern().permanentBuff()).hasToString(expected.get("tavernBuff").asString());
        }
        if (expected.has("relicSlots")) {
            assertThat(state.tavern().relicSlots()).as("relic slots").hasSize(expected.get("relicSlots").asInt());
        }
        if (expected.has("band")) {
            List<Unit> units = state.band().units();
            assertThat(units).as("band size").hasSize(expected.get("band").size());
            int i = 0;
            for (JsonNode spec : expected.get("band")) {
                Unit unit = units.get(i++);
                if (spec.has("hero")) {
                    assertThat(unit.heroId()).as("hero at " + (i - 1)).isEqualTo(spec.get("hero").asString());
                }
                if (spec.has("attack")) {
                    assertThat(unit.attack()).as("attack of " + unit).isEqualTo(spec.get("attack").asInt());
                }
                if (spec.has("health")) {
                    assertThat(unit.health()).as("health of " + unit).isEqualTo(spec.get("health").asInt());
                }
                if (spec.has("level")) {
                    assertThat(unit.level()).as("level of " + unit).isEqualTo(spec.get("level").asInt());
                }
                if (spec.has("perk")) {
                    assertThat(unit.perk().map(Enum::name).orElse("none")).as("perk of " + unit).isEqualTo(spec.get("perk").asString());
                }
            }
        }
    }

    private static String story(List<GameEvent> events) {
        StringBuilder text = new StringBuilder();
        int n = 0;
        for (GameEvent event : events) {
            text.append('#').append(n++).append(' ').append(event).append('\n');
        }
        return text.toString();
    }
}
