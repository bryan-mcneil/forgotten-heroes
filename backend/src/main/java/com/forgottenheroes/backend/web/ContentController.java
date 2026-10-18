package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.service.ContentService;
import com.forgottenheroes.backend.web.dto.ContentDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/content}: heroes, relics and tokens as the engine loaded them. No auth (SecurityConfig.PUBLIC). */
@RestController
public class ContentController {

    private final ContentService content;

    public ContentController(ContentService content) {
        this.content = content;
    }

    // the empty list tells the OpenAPI contract this door needs no token: the title screen loads it before login
    @SecurityRequirements
    @GetMapping("/api/content")
    public ContentDto content() {
        return content.content();
    }
}
