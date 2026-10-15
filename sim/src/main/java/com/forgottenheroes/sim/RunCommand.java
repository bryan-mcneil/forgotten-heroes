package com.forgottenheroes.sim;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** {@code sim run --games N --bot greedy|random --seed S --out target/sim/report.json}: play and report. */
@Command(name = "run", description = "Play N bot games and write the balance report (JSON + Markdown).")
public final class RunCommand implements Callable<Integer> {

    @Option(names = "--games", description = "how many games the measured bot plays (default ${DEFAULT-VALUE})", defaultValue = "1000")
    int games;

    @Option(names = "--bot", description = "greedy or random (default ${DEFAULT-VALUE})", defaultValue = "greedy")
    String bot;

    @Option(names = "--seed", description = "seed of the whole simulation (default ${DEFAULT-VALUE})", defaultValue = "1")
    long seed;

    @Option(names = "--out", description = "JSON report path; the Markdown goes next to it (default ${DEFAULT-VALUE})",
        defaultValue = "target/sim/report.json")
    Path out;

    @Option(names = "--title", description = "title of the Markdown report", defaultValue = "Balance report")
    String title;

    @Override
    public Integer call() throws Exception {
        long started = System.nanoTime();
        Report report = new Simulation().play(bot, games, seed);
        Json.write(out, report);
        Path markdown = out.resolveSibling(out.getFileName().toString().replaceAll("\\.json$", "") + ".md");
        Files.writeString(markdown, ReportMarkdown.render(report, title), StandardCharsets.UTF_8);
        double seconds = (System.nanoTime() - started) / 1e9;
        System.out.printf(Locale.ROOT, "%d %s games in %.1f s: avg %.1f turns, %.0f%% runs won, %.0f%% battles won, %s greedy-vs-random, "
                + "median %.0f clashes, p99.9 %.0f, %d clash-cap draws%n",
            report.games(), report.bot(), seconds, report.avgTurns(), report.runsWonRate() * 100, report.battleWinRate() * 100,
            report.greedyVsRandomWinRate() == null ? "n/a" : String.format(Locale.ROOT, "%.0f%%", report.greedyVsRandomWinRate() * 100),
            report.medianClashes(), report.p999Clashes(), report.clashCapDraws());
        System.out.println("report: " + out + " and " + markdown);
        return 0;
    }
}
