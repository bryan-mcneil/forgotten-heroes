package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.service.ProfileService;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import com.forgottenheroes.backend.web.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/me}: the signed-in player's profile and the run to resume. {@code PUT /api/me}: choose the display name. */
@RestController
public class ProfileController {

    private final ProfileService profiles;
    private final CurrentUser user;

    public ProfileController(ProfileService profiles, CurrentUser user) {
        this.profiles = profiles;
        this.user = user;
    }

    @GetMapping("/api/me")
    public ProfileDto me() {
        return profiles.me(user.id());
    }

    /** The name other players see on this player's ghosts and leaderboard rows; 400 when it breaks the naming rules. */
    @PutMapping("/api/me")
    public ProfileDto rename(@Valid @RequestBody UpdateProfileRequest request) {
        return profiles.rename(user.id(), request.displayName());
    }
}
