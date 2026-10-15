package com.forgottenheroes.sim;

import com.forgottenheroes.engine.bot.RunSummary;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.HeroDefinition;
import com.forgottenheroes.engine.model.Outcome;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.model.Rules;
import com.forgottenheroes.engine.session.UnitSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The numbers GDD §9 asks about, computed from a list of bot runs: run length, win rates, hero
 * pick rate and win rate when on the band, average stats per turn, battle length distribution,
 * clash-cap draws and the gold each economy hero earns. Written as JSON for {@code check} and as
 * Markdown for people.
 */
public record Report(String bot, long seed, int games, int poolGames, double avgTurns, double runsWonRate, double battleWinRate,
                     Double greedyVsRandomWinRate, int battles, double medianClashes, double p999Clashes, int clashCapDraws,
                     Map<String, HeroStats> heroes, List<TurnStats> turns, Map<Integer, Integer> clashHistogram,
                     Map<Integer, Integer> runLengthHistogram) {

    /** One hero across all games: how often it was played and how it fared when it was. */
    public record HeroStats(int rank, double pickRate, double winRateOnBand, int battlesOnBand, double goldPerGame) { }

    /** The average band on one turn: size and stats per unit. */
    public record TurnStats(int turn, int bands, double avgSize, double avgAttack, double avgHealth) { }

    public Report {
        heroes = new TreeMap<>(heroes);
        turns = List.copyOf(turns);
        clashHistogram = new TreeMap<>(clashHistogram);
        runLengthHistogram = new TreeMap<>(runLengthHistogram);
    }

    public static Report of(ContentIndex content, String bot, long seed, int poolGames, List<RunSummary> runs, Double greedyVsRandom) {
        int games = runs.size();
        double avgTurns = runs.stream().mapToInt(RunSummary::turns).average().orElse(0);
        double runsWonRate = rate(runs.stream().filter(run -> run.status() == RunStatus.WON).count(), games);
        long battles = runs.stream().mapToLong(run -> run.battles().size()).sum();
        long wins = runs.stream().mapToLong(RunSummary::wins).sum();

        Map<String, Integer> gamesWith = new HashMap<>();
        Map<String, Integer> battlesWith = new HashMap<>();
        Map<String, Integer> winsWith = new HashMap<>();
        Map<String, Integer> gold = new HashMap<>();
        Map<Integer, List<RunSummary.TurnRecord>> byTurn = new TreeMap<>();
        List<Integer> clashes = new ArrayList<>();
        Map<Integer, Integer> clashHistogram = new TreeMap<>();
        Map<Integer, Integer> runLengths = new TreeMap<>();
        int capDraws = 0;
        for (RunSummary run : runs) {
            Set<String> seen = new HashSet<>();
            runLengths.merge(run.turns(), 1, Integer::sum);
            run.goldFromAbilities().forEach((hero, amount) -> gold.merge(hero, amount, Integer::sum));
            for (RunSummary.TurnRecord record : run.battles()) {
                byTurn.computeIfAbsent(record.turn(), key -> new ArrayList<>()).add(record);
                clashes.add(record.clashes());
                clashHistogram.merge(record.clashes(), 1, Integer::sum);
                if (record.outcome() == Outcome.DRAW && record.clashes() >= Rules.CLASH_CAP) {
                    capDraws++;
                }
                Set<String> onBand = new HashSet<>();
                for (UnitSnapshot unit : record.band().units()) {
                    onBand.add(unit.heroId());
                }
                for (String hero : onBand) {
                    seen.add(hero);
                    battlesWith.merge(hero, 1, Integer::sum);
                    if (record.outcome() == Outcome.WIN) {
                        winsWith.merge(hero, 1, Integer::sum);
                    }
                }
            }
            for (String hero : seen) {
                gamesWith.merge(hero, 1, Integer::sum);
            }
        }
        Map<String, HeroStats> heroes = new TreeMap<>();
        for (HeroDefinition hero : content.heroes()) {
            int played = gamesWith.getOrDefault(hero.id(), 0);
            int fought = battlesWith.getOrDefault(hero.id(), 0);
            heroes.put(hero.id(), new HeroStats(hero.rank().number(), rate(played, games),
                rate(winsWith.getOrDefault(hero.id(), 0), fought), fought,
                played == 0 ? 0 : (double) gold.getOrDefault(hero.id(), 0) / played));
        }
        List<TurnStats> turns = new ArrayList<>();
        byTurn.forEach((turn, records) -> {
            int units = records.stream().mapToInt(record -> record.band().size()).sum();
            int attack = records.stream().flatMap(record -> record.band().units().stream()).mapToInt(UnitSnapshot::attack).sum();
            int health = records.stream().flatMap(record -> record.band().units().stream()).mapToInt(UnitSnapshot::health).sum();
            turns.add(new TurnStats(turn, records.size(), (double) units / records.size(),
                units == 0 ? 0 : (double) attack / units, units == 0 ? 0 : (double) health / units));
        });
        clashes.sort(null);
        return new Report(bot, seed, games, poolGames, avgTurns, runsWonRate, rate(wins, battles), greedyVsRandom, (int) battles,
            percentile(clashes, 0.5), percentile(clashes, 0.999), capDraws, heroes, turns, clashHistogram, runLengths);
    }

    private static double rate(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }

    private static double percentile(List<Integer> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
