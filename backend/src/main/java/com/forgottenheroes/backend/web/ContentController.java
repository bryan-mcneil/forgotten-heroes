package com.forgottenheroes.backend.web;

import com.forgottenheroes.backend.service.ContentService;
import com.forgottenheroes.backend.web.dto.ContentDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/content}: heroes, relics and tokens as the engine loaded them. No auth. */
@RestController
public class ContentController {

    private final ContentService content;

    public ContentController(ContentService content) {
        this.content = content;
    }

    @GetMapping("/api/content")
    public ContentDto content() {
        return content.content();
    }
}
