package com.forgottenheroes.backend.web.dto;

/** The signed-in player: name, best result, how many runs, and the run to resume (null if none). */
public record ProfileDto(String displayName, int bestCrowns, int runsPlayed, String activeRunId) {
}
