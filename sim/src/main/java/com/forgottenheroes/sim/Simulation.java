package com.forgottenheroes.sim;

import com.forgottenheroes.engine.bot.Bot;
import com.forgottenheroes.engine.bot.BotRunner;
import com.forgottenheroes.engine.bot.GhostPool;
import com.forgottenheroes.engine.bot.GhostProvider;
import com.forgottenheroes.engine.bot.GreedyBot;
import com.forgottenheroes.engine.bot.RandomBot;
import com.forgottenheroes.engine.bot.RunSummary;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.session.GameSession;
import java.util.ArrayList;
import java.util.List;

/**
 * The protocol behind {@code sim run}: a pool of random-bot bands first (so turn 1 has opponents),
 * then the measured bot's games, each one adding its bands to the pool so bots fight bots. When the
 * measured bot is the greedy one, a side tournament against random-only ghosts gives the GDD §9
 * "greedy vs random" number.
 */
public final class Simulation {

    private final ContentIndex content;
    private final BotRunner runner;

    public Simulation() {
        this(ContentLoader.load());
    }

    public Simulation(ContentIndex content) {
        this.content = content;
        this.runner = new BotRunner(new GameSession(content));
    }

    public ContentIndex content() {
        return content;
    }

    public static Bot bot(String name) {
        return switch (name.toLowerCase()) {
            case "greedy" -> new GreedyBot();
            case "random" -> new RandomBot();
            default -> throw new IllegalArgumentException("unknown bot '" + name + "': use greedy or random");
        };
    }

    public Report play(String botName, int games, long seed) {
        Bot bot = bot(botName);
        int poolGames = Math.max(10, games / 10);
        GhostPool randomOnly = new GhostPool();
        GhostPool pool = new GhostPool();
        for (int i = 1; i <= poolGames; i++) {
            RunSummary warmup = runner.playRun(new RandomBot(), seed * 1_000_003L + i, GhostProvider.nobody());
            randomOnly.addAll(warmup);
            pool.addAll(warmup);
        }
        List<RunSummary> runs = new ArrayList<>(games);
        for (int i = 1; i <= games; i++) {
            RunSummary run = runner.playRun(bot, seed * 7_919L + i, pool);
            pool.addAll(run);
            runs.add(run);
        }
        Double greedyVsRandom = null;
        if (bot instanceof GreedyBot) {
            long wins = 0;
            long battles = 0;
            int tournament = Math.min(games, 200);
            for (int i = 1; i <= tournament; i++) {
                RunSummary run = runner.playRun(bot, seed * 104_729L + i, randomOnly);
                wins += run.wins();
                battles += run.battles().size();
            }
            greedyVsRandom = battles == 0 ? 0 : (double) wins / battles;
        }
        return Report.of(content, bot.name(), seed, poolGames, runs, greedyVsRandom);
    }

    public BotRunner runner() {
        return runner;
    }
}
