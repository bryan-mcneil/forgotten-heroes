package com.forgottenheroes.engine.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.forgottenheroes.engine.event.EventJson;
import com.forgottenheroes.engine.model.ItemKind;
import org.junit.jupiter.api.Test;

class ActionTest {

    @Test
    void negativeIndexesAreRejectedAtConstruction() {
        assertThatThrownBy(() -> new Action.Freeze(ItemKind.HERO, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Freeze(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Unfreeze(ItemKind.RELIC, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Buy(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.BuyRelic(0, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Sell(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Move(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Action.Merge(1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionsAreDataWithATypeField() {
        assertThat(EventJson.mapper().writeValueAsString(new Action.Buy(1, 0))).isEqualTo("{\"type\":\"Buy\",\"slot\":1,\"bandIndex\":0}");
        assertThat(EventJson.mapper().writeValueAsString(new Action.BuyRelic(0, null))).isEqualTo("{\"type\":\"BuyRelic\",\"slot\":0}");
        assertThat(EventJson.mapper().readValue("{\"type\":\"Freeze\",\"kind\":\"RELIC\",\"slot\":0}", Action.class))
            .isEqualTo(new Action.Freeze(ItemKind.RELIC, 0));
        assertThat(EventJson.mapper().readValue("{\"type\":\"EndTurn\"}", Action.class)).isEqualTo(new Action.EndTurn());
    }
}
