package com.forgottenheroes.sim;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** {@code sim check --report target/sim/report.json --thresholds sim/thresholds.yaml}: exit 1 when a GDD §9 target is missed. */
@Command(name = "check", description = "Compare a report with the balance thresholds; exit 1 on any failure.")
public final class CheckCommand implements Callable<Integer> {

    @Option(names = "--report", description = "JSON report written by run", defaultValue = "target/sim/report.json")
    Path report;

    @Option(names = "--thresholds", description = "YAML thresholds (default ${DEFAULT-VALUE})", defaultValue = "sim/thresholds.yaml")
    Path thresholds;

    @Override
    public Integer call() throws Exception {
        Report loaded = Json.read(report, Report.class);
        Thresholds targets = Json.readYaml(thresholds, Thresholds.class);
        List<Thresholds.Finding> findings = targets.check(loaded);
        long failed = findings.stream().filter(finding -> !finding.passed()).count();
        for (Thresholds.Finding finding : findings) {
            System.out.println((finding.passed() ? "PASS " : "FAIL ") + finding.name() + ": " + finding.detail());
        }
        System.out.println(failed == 0 ? "all " + findings.size() + " thresholds hold" : failed + " of " + findings.size() + " thresholds fail");
        return failed == 0 ? 0 : 1;
    }
}
