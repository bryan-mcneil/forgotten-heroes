/**
 * The balance simulator. {@code SimMain} is the command line ({@code run}, {@code check},
 * {@code fuzz}, {@code replay}); {@code Simulation} plays the games, {@code Report} measures them,
 * {@code Thresholds} says what GDD §9 calls balanced. It only ever talks to the engine through
 * {@code GameSession} and the bots.
 */
package com.forgottenheroes.sim;
