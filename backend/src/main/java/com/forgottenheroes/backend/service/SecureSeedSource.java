package com.forgottenheroes.backend.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Unpredictable seeds, so no player can guess another run's shop. */
@Component
public class SecureSeedSource implements SeedSource {

    private final SecureRandom random = new SecureRandom();

    @Override
    public long next() {
        return random.nextLong();
    }
}
