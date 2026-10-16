package com.forgottenheroes.backend.service;

import com.forgottenheroes.backend.web.dto.ContentDto;
import com.forgottenheroes.engine.content.ContentIndex;
import org.springframework.stereotype.Service;

/** The content files the engine loaded, for the client to draw from. */
@Service
public class ContentService {

    private final ContentIndex content;

    public ContentService(ContentIndex content) {
        this.content = content;
    }

    public ContentDto content() {
        return new ContentDto(content.heroes(), content.relics(), content.tokens());
    }
}
