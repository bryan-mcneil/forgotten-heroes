package com.forgottenheroes.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The balance targets of GDD §9 as numbers ({@code sim/thresholds.yaml}). {@link #check} turns a
 * report into a list of pass/fail lines; {@code sim check} exits 1 when any line fails.
 */
public record Thresholds(Range runLength, Range greedyVsRandom, Range heroWinRateOnBand, Range pickRate, Range medianClashes,
                         Range p999Clashes, Range clashCapDraws, int minBattlesForHeroRates, Map<String, Double> minGoldPerGame) {

    public record Range(Double min, Double max) {
        boolean holds(double value) {
            return (min == null || value >= min) && (max == null || value <= max);
        }

        @Override
        public String toString() {
            return (min == null ? "" : min) + ".." + (max == null ? "" : max);
        }
    }

    public record Finding(String name, boolean passed, String detail) { }

    public Thresholds {
        minGoldPerGame = minGoldPerGame == null ? Map.of() : Map.copyOf(minGoldPerGame);
    }

    public List<Finding> check(Report report) {
        List<Finding> findings = new ArrayList<>();
        range(findings, "run length", runLength, report.avgTurns());
        if (report.greedyVsRandomWinRate() != null) {
            range(findings, "greedy vs random", greedyVsRandom, report.greedyVsRandomWinRate());
        }
        range(findings, "median clashes", medianClashes, report.medianClashes());
        range(findings, "p99.9 clashes", p999Clashes, report.p999Clashes());
        range(findings, "clash-cap draws", clashCapDraws, report.clashCapDraws());
        report.heroes().forEach((id, stats) -> {
            if (stats.battlesOnBand() >= minBattlesForHeroRates) {
                range(findings, "win rate on band: " + id, heroWinRateOnBand, stats.winRateOnBand());
            }
            range(findings, "pick rate: " + id, pickRate, stats.pickRate());
        });
        minGoldPerGame.forEach((hero, min) -> {
            double actual = report.heroes().containsKey(hero) ? report.heroes().get(hero).goldPerGame() : 0;
            findings.add(new Finding("gold per game: " + hero, actual >= min,
                String.format(Locale.ROOT, "%.1f (min %.1f)", actual, min)));
        });
        return findings;
    }

    private static void range(List<Finding> findings, String name, Range range, double value) {
        if (range == null) {
            return;
        }
        findings.add(new Finding(name, range.holds(value), String.format(Locale.ROOT, "%.3f (want %s)", value, range)));
    }
}
