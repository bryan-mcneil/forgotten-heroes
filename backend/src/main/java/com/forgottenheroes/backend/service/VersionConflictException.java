package com.forgottenheroes.backend.service;

/**
 * The client acted on a run state older than the one the server holds (architecture §4.3): answered
 * with 409 so the client reloads. Double submits of the same action land here and do no harm.
 */
public class VersionConflictException extends RuntimeException {

    private final long expected;
    private final long actual;

    public VersionConflictException(String runId, long expected, long actual) {
        super("Run " + runId + " changed since you last loaded it: you sent version " + expected
            + ", the server has version " + actual + ". Reload and try again.");
        this.expected = expected;
        this.actual = actual;
    }

    public long expected() {
        return expected;
    }

    public long actual() {
        return actual;
    }
}
