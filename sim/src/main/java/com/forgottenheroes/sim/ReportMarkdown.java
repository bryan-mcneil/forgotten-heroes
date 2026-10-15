package com.forgottenheroes.sim;

import java.util.Locale;
import java.util.Map;

/** Renders a {@link Report} as the Markdown page that goes under {@code docs/balance/}. */
public final class ReportMarkdown {

    private ReportMarkdown() { }

    public static String render(Report report, String title) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(title).append("\n\n");
        md.append(String.format(Locale.ROOT, "Bot `%s`, seed %d, %d games (+ %d random warm-up games for the ghost pool).%n\n",
            report.bot(), report.seed(), report.games(), report.poolGames()));
        md.append("## Summary\n\n| Measure | Value | GDD §9 target |\n|---|---|---|\n");
        row(md, "Average run length (turns)", f1(report.avgTurns()), "8–12");
        row(md, "Runs won (10 crowns)", pct(report.runsWonRate()), "—");
        row(md, "Battle win rate vs the pool", pct(report.battleWinRate()), "—");
        row(md, "Greedy vs random-bot ghosts", report.greedyVsRandomWinRate() == null ? "n/a" : pct(report.greedyVsRandomWinRate()), "75–90 %");
        row(md, "Battles played", String.valueOf(report.battles()), "—");
        row(md, "Median battle length (clashes)", f1(report.medianClashes()), "6–14");
        row(md, "99.9th percentile battle length", f1(report.p999Clashes()), "< 60");
        row(md, "Clash-cap draws", String.valueOf(report.clashCapDraws()), "0");
        md.append("\n## Heroes\n\nPick rate = share of games where the hero stood on the band at the end of a turn; win rate = battles won while it stood there.\n\n");
        md.append("| Hero | Rank | Pick rate | Win rate on band | Battles | Gold per game |\n|---|---|---|---|---|---|\n");
        report.heroes().forEach((id, stats) -> md.append(String.format(Locale.ROOT, "| %s | %d | %s | %s | %d | %s |\n",
            id, stats.rank(), pct(stats.pickRate()), pct(stats.winRateOnBand()), stats.battlesOnBand(), f1(stats.goldPerGame()))));
        md.append("\n## Bands per turn\n\n| Turn | Bands | Avg size | Avg attack | Avg health |\n|---|---|---|---|---|\n");
        for (Report.TurnStats turn : report.turns()) {
            md.append(String.format(Locale.ROOT, "| %d | %d | %s | %s | %s |\n", turn.turn(), turn.bands(), f1(turn.avgSize()),
                f1(turn.avgAttack()), f1(turn.avgHealth())));
        }
        md.append("\n## Battle length\n\n| Clashes | Battles |\n|---|---|\n");
        for (Map.Entry<Integer, Integer> bucket : report.clashHistogram().entrySet()) {
            md.append("| ").append(bucket.getKey()).append(" | ").append(bucket.getValue()).append(" |\n");
        }
        md.append("\n## Run length\n\n| Turns | Runs |\n|---|---|\n");
        for (Map.Entry<Integer, Integer> bucket : report.runLengthHistogram().entrySet()) {
            md.append("| ").append(bucket.getKey()).append(" | ").append(bucket.getValue()).append(" |\n");
        }
        return md.toString();
    }

    private static void row(StringBuilder md, String name, String value, String target) {
        md.append("| ").append(name).append(" | ").append(value).append(" | ").append(target).append(" |\n");
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.1f %%", value * 100);
    }

    private static String f1(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
