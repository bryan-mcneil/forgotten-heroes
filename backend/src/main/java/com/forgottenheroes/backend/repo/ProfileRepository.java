package com.forgottenheroes.backend.repo;

import java.util.Optional;

/** One profile per user id. */
public interface ProfileRepository {

    Optional<Profile> find(String userId);

    void save(Profile profile);
}
