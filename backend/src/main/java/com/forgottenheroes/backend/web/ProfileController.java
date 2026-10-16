package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.security.CurrentUser;
import com.forgottenheroes.backend.service.ProfileService;
import com.forgottenheroes.backend.web.dto.ProfileDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/me}: the signed-in player's profile and the run to resume. */
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
}
