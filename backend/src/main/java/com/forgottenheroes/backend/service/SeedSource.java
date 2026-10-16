package com.forgottenheroes.backend.service;

/**
 * Where a new run's seed comes from. The engine must never draw randomness itself (ADR-4); the
 * backend draws one number per run here and the engine derives every roll and battle from it. An
 * interface so a test can hand in a fixed seed and replay the same run.
 */
public interface SeedSource {

    long next();
}
