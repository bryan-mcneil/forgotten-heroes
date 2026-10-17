package com.forgottenheroes.backend.repo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Profiles in a map, gone when the app stops: the default, until {@code app.storage=dynamo} swaps it out. */
@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryProfileRepository implements ProfileRepository {

    private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<Profile> find(String userId) {
        return Optional.ofNullable(profiles.get(userId));
    }

    @Override
    public void save(Profile profile) {
        profiles.put(profile.userId(), profile);
    }
}
