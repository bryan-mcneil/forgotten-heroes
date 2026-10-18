package com.forgottenheroes.backend.web.dto;

import com.forgottenheroes.backend.service.DisplayNamePolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/me}: the name other players see. The size here is for the contract and the
 * first 400; the full rules (characters, blocklist, space collapsing) live in {@link DisplayNamePolicy}.
 */
public record UpdateProfileRequest(
    @NotBlank @Size(min = DisplayNamePolicy.MIN, max = DisplayNamePolicy.MAX) String displayName) {
}
