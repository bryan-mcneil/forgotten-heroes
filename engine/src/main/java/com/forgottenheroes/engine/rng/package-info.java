/**
 * Randomness you can replay. {@code GameRandom} is the only dice in the engine; every seed comes
 * from {@code SeedDerivation}, and both lean on the {@code Hash64} mixer. No {@code Math.random()},
 * no {@code new Random()}, no clocks: ArchUnit enforces this from Step 23.
 */
package com.forgottenheroes.engine.rng;
