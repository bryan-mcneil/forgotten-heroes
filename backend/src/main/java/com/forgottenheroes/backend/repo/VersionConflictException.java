package com.forgottenheroes.backend.repo;

/**
 * The run changed under the caller's feet (architecture §4.3): the version the client sent, or the
 * one the service loaded, is no longer the one in storage. Thrown by the service's quick check and
 * by the repository's conditional write; answered with 409 so the client reloads. Double submits of
 * the same action land here and do no harm.
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
