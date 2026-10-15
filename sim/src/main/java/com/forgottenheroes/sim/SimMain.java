package com.forgottenheroes.sim;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/** The entry point: {@code sim run | check | fuzz | replay}. A non-zero exit code means a failed check or a found bug. */
@Command(name = "sim", mixinStandardHelpOptions = true, version = "forgotten-heroes sim 0.1",
    description = "Plays Forgotten Heroes with bots and reports on balance.",
    subcommands = {RunCommand.class, CheckCommand.class, FuzzCommand.class, ReplayCommand.class})
public final class SimMain implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static int execute(String... args) {
        return new CommandLine(new SimMain()).execute(args);
    }

    public static void main(String[] args) {
        int code = execute(args);
        if (code != 0) {
            System.exit(code);
        }
    }
}
