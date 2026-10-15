package com.forgottenheroes.engine.model;

/** Where a run stands. Only IN_PROGRESS accepts actions. */
public enum RunStatus {
    IN_PROGRESS, WON, LOST, ABANDONED;

    public boolean isOver() {
        return this != IN_PROGRESS;
    }
}
