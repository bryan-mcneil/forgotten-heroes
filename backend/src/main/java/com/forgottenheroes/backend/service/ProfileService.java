package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.repo.Profile;
import com.forgottenheroes.backend.repo.ProfileRepository;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import java.time.Instant;
import org.springframework.stereotype.Service;

/** The signed-in player's profile; created on first call (architecture §4.2), renamed with {@code PUT /api/me}. */
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
        return toDto(getOrCreate(userId));
    }

    /**
     * The name other players see on this player's ghosts and leaderboard rows from now on. Rows and
     * ghosts written before keep the old name: they carry their own copy (Step 32, no joins).
     */
    public ProfileDto rename(String userId, String requestedName) {
        String name = DisplayNamePolicy.clean(requestedName);
        Profile renamed = getOrCreate(userId).withDisplayName(name);
        profiles.save(renamed);
        return toDto(renamed);
    }

    private static ProfileDto toDto(Profile profile) {
        return new ProfileDto(profile.displayName(), profile.bestCrowns(), profile.runsPlayed(), profile.activeRunId());
    }
}
