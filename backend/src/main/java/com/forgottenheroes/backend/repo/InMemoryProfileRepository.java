package com.forgottenheroes.backend.repo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Profiles in a map, gone when the app stops. */
@Repository
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
