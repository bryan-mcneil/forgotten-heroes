package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.repo.Profile;
import com.forgottenheroes.backend.repo.ProfileRepository;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** The signed-in player's profile; created on first call (architecture §4.2). */
@Service
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    public Profile getOrCreate(String userId) {
        return profiles.find(userId).orElseGet(() -> {
            Profile created = Profile.create(userId, Instant.now());
            profiles.save(created);
            return created;
        });
    }

    public ProfileDto me(String userId) {
        Profile profile = getOrCreate(userId);
        return new ProfileDto(profile.displayName(), profile.bestCrowns(), profile.runsPlayed(), profile.activeRunId());
    }
}
