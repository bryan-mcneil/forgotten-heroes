package com.forgottenheroes.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.session.GameSession;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimulationTest {

    private static final Simulation SIMULATION = new Simulation();

    @Test
    void aSmallRunProducesACompleteReport() {
        Report report = SIMULATION.play("greedy", 30, 3);
        assertThat(report.games()).isEqualTo(30);
        assertThat(report.bot()).isEqualTo("greedy");
        assertThat(report.battles()).isGreaterThan(100);
        assertThat(report.heroes()).hasSize(36);
        assertThat(report.heroes().values().stream().mapToDouble(Report.HeroStats::pickRate).sum()).isGreaterThan(1);
        assertThat(report.turns()).isNotEmpty();
        assertThat(report.turns().getFirst().turn()).isEqualTo(1);
        assertThat(report.greedyVsRandomWinRate()).isNotNull().isGreaterThan(0.5);
        assertThat(report.clashHistogram().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(report.battles());
        assertThat(report.runLengthHistogram().values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(30);
        assertThat(report.medianClashes()).isLessThanOrEqualTo(report.p999Clashes());
    }

    @Test
    void theRandomBotReportHasNoGreedyNumber() {
        Report report = SIMULATION.play("random", 12, 5);
        assertThat(report.greedyVsRandomWinRate()).isNull();
        assertThat(report.games()).isEqualTo(12);
        assertThatThrownBy(() -> Simulation.bot("clever")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSameSeedGivesTheSameReport() {
        assertThat(SIMULATION.play("greedy", 15, 9)).isEqualTo(SIMULATION.play("greedy", 15, 9));
    }

    @Test
    void markdownListsEveryHeroAndTheSummary() {
        Report report = SIMULATION.play("greedy", 15, 1);
        String markdown = ReportMarkdown.render(report, "Test report");
        assertThat(markdown).startsWith("# Test report").contains("| merchant | 1 |").contains("Clash-cap draws").contains("## Bands per turn");
    }

    @Test
    void thresholdsReportPassAndFailLines(@TempDir Path dir) throws Exception {
        Report report = SIMULATION.play("greedy", 20, 1);
        Thresholds targets = Json.readYaml(Path.of("thresholds.yaml"), Thresholds.class);
        List<Thresholds.Finding> findings = targets.check(report);
        assertThat(findings).isNotEmpty();
        assertThat(findings).anyMatch(finding -> finding.name().startsWith("pick rate: "));
        Thresholds lenient = new Thresholds(new Thresholds.Range(0.0, 100.0), null, null, null, null, null, null, 1_000_000, null);
        assertThat(lenient.check(report)).allMatch(Thresholds.Finding::passed);
        Thresholds strict = new Thresholds(new Thresholds.Range(100.0, null), null, null, null, null, null, null, 1_000_000, null);
        assertThat(strict.check(report)).anyMatch(finding -> !finding.passed());
    }

    @Test
    void theCommandsWriteReadAndJudgeReports(@TempDir Path dir) throws Exception {
        Path out = dir.resolve("report.json");
        assertThat(SimMain.execute("run", "--games", "12", "--bot", "greedy", "--seed", "2", "--out", out.toString())).isZero();
        assertThat(Files.exists(out)).isTrue();
        assertThat(Files.exists(dir.resolve("report.md"))).isTrue();
        Report back = Json.read(out, Report.class);
        assertThat(back.games()).isEqualTo(12);
        assertThat(SimMain.execute("check", "--report", out.toString(), "--thresholds", "thresholds.yaml")).isEqualTo(1);
        Path lenient = dir.resolve("lenient.yaml");
        Files.writeString(lenient, "runLength:\n  min: 0\n  max: 100\nminBattlesForHeroRates: 1000000\n", StandardCharsets.UTF_8);
        assertThat(SimMain.execute("check", "--report", out.toString(), "--thresholds", lenient.toString())).isZero();
        assertThat(SimMain.execute("--help")).isZero();
        assertThat(SimMain.execute()).isZero();
    }

    @Test
    void fuzzingFindsNoEngineExceptions() {
        FuzzCommand.Outcome outcome = FuzzCommand.fuzz(new GameSession(ContentLoader.load()), 20_000, 7);
        assertThat(outcome.clean()).as(outcome.failure()).isTrue();
        assertThat(outcome.tried()).isEqualTo(20_000);
        assertThat(outcome.accepted()).isGreaterThan(1000);
        assertThat(outcome.refused()).isGreaterThan(1000);
        assertThat(outcome.runs()).isGreaterThan(1);
        assertThat(SimMain.execute("fuzz", "--actions", "2000", "--seed", "3")).isZero();
    }

    @Test
    void replayPrintsTheStoryAndRefusals(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bug.json");
        Files.writeString(file, "{\"seed\": 42, \"actions\": [{\"type\":\"Buy\",\"slot\":0,\"bandIndex\":0}, {\"type\":\"Sell\",\"bandIndex\":4}, {\"type\":\"EndTurn\"}]}",
            StandardCharsets.UTF_8);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        FuzzCommand.Replay replay = Json.read(file, FuzzCommand.Replay.class);
        int code = ReplayCommand.replay(new GameSession(ContentLoader.load()), replay.seed(), replay.actions(), new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String story = bytes.toString(StandardCharsets.UTF_8);
        assertThat(code).isZero();
        assertThat(story).contains("--- action 1").contains("HeroBought").contains("refused: INVALID_INDEX").contains("BattleEnded").contains("--- final");
        assertThat(SimMain.execute("replay", "--file", file.toString())).isZero();
    }
}
