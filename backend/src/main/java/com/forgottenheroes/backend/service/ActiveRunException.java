package com.forgottenheroes.backend.service;

/** {@code POST /runs} while a run is still in progress: answered with 409 and the run to resume. */
public class ActiveRunException extends RuntimeException {

    private final String activeRunId;

    public ActiveRunException(String activeRunId) {
        super("A run is already in progress: " + activeRunId + ". Finish or abandon it first.");
        this.activeRunId = activeRunId;
    }

    public String activeRunId() {
        return activeRunId;
    }
}
