package com.forgottenheroes.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The naming rules, one at a time, plus the shape of the refusal. */
class DisplayNamePolicyTest {

    @Test
    void aGoodNameIsTrimmedAndItsSpacesCollapsed() {
        assertThat(DisplayNamePolicy.clean("  Aoi   the Brave ")).isEqualTo("Aoi the Brave");
        assertThat(DisplayNamePolicy.clean("Aoi\tBrave")).as("any whitespace becomes one space").isEqualTo("Aoi Brave");
        assertThat(DisplayNamePolicy.clean("Bryan2026")).isEqualTo("Bryan2026");
        assertThat(DisplayNamePolicy.clean("Cassandra")).as("a blocked word inside a longer word is fine").isEqualTo("Cassandra");
        assertThat(DisplayNamePolicy.clean("Hero e498")).as("the default name passes its own rules").isEqualTo("Hero e498");
    }

    @Test
    void lengthIsCheckedAfterTheSpacesAreCollapsed() {
        assertThatThrownBy(() -> DisplayNamePolicy.clean("ab")).hasMessageContaining("3 to 16");
        assertThatThrownBy(() -> DisplayNamePolicy.clean("    a   ")).as("padding does not count").hasMessageContaining("3 to 16");
        assertThat(DisplayNamePolicy.clean("a       b")).as("collapses to exactly three characters").isEqualTo("a b");
        assertThatThrownBy(() -> DisplayNamePolicy.clean("abcdefghijklmnopq")).hasMessageContaining("3 to 16");
        assertThatThrownBy(() -> DisplayNamePolicy.clean(null)).hasMessageContaining("3 to 16");
        assertThat(DisplayNamePolicy.clean("abc")).isEqualTo("abc");
        assertThat(DisplayNamePolicy.clean("abcdefghijklmnop")).hasSize(16);
    }

    @Test
    void onlyLettersDigitsAndSpaces() {
        for (String bad : List.of("Aoi!", "Aoi_the_Brave", "Aoi-chan", "葵の勇者", "Aoi@home")) {
            assertThatThrownBy(() -> DisplayNamePolicy.clean(bad)).as(bad).hasMessageContaining("letters, digits and spaces");
        }
    }

    @Test
    void blockedWordsAreRefusedWholeOrSpacedOut() {
        for (String bad : List.of("Admin", "ADMIN", "a d m i n", "the admin", "Moderator Bob")) {
            assertThatThrownBy(() -> DisplayNamePolicy.clean(bad)).as(bad)
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not available")
                .extracting("code").isEqualTo("INVALID_DISPLAY_NAME");
        }
    }
}
