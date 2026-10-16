package com.forgottenheroes.backend.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentLoader;
import com.forgottenheroes.engine.model.ItemKind;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.model.RunStatus;
import com.forgottenheroes.engine.session.GameSession;
import org.junit.jupiter.api.Test;

class DtoMapperTest {

    private static final GameSession SESSION = new GameSession(ContentLoader.load());

    @Test
    void copiesAFreshRunFaithfully() {
        RunState state = SESSION.start("run-1", 42L).state();

        RunStateDto dto = DtoMapper.toDto(state);

        assertThat(dto.runId()).isEqualTo("run-1");
        assertThat(dto.turn()).isEqualTo(1);
        assertThat(dto.gold()).isEqualTo(state.gold());
        assertThat(dto.hearts()).isEqualTo(5);
        assertThat(dto.crowns()).isZero();
        assertThat(dto.status()).isEqualTo(RunStatus.IN_PROGRESS);
        assertThat(dto.band().units()).isEmpty();
        assertThat(dto.tavern().heroes()).hasSameSizeAs(state.tavern().heroSlots());
        assertThat(dto.tavern().heroes()).allSatisfy(slot -> assertThat(slot.itemId()).isNotNull());
        assertThat(dto.lastBattle()).isNull();
        assertThat(dto.version()).isEqualTo(state.version());
    }

    @Test
    void mapsEveryRequestShapeToTheEngineAction() {
        assertThat(DtoMapper.toAction(new ActionRequest.Roll(1L))).isEqualTo(new Action.Roll());
        assertThat(DtoMapper.toAction(new ActionRequest.Freeze(ItemKind.HERO, 2, 1L))).isEqualTo(new Action.Freeze(ItemKind.HERO, 2));
        assertThat(DtoMapper.toAction(new ActionRequest.Unfreeze(ItemKind.RELIC, 0, 1L))).isEqualTo(new Action.Unfreeze(ItemKind.RELIC, 0));
        assertThat(DtoMapper.toAction(new ActionRequest.Buy(0, 3, 1L))).isEqualTo(new Action.Buy(0, 3));
        assertThat(DtoMapper.toAction(new ActionRequest.BuyRelic(1, null, 1L))).isEqualTo(new Action.BuyRelic(1, null));
        assertThat(DtoMapper.toAction(new ActionRequest.Sell(4, 1L))).isEqualTo(new Action.Sell(4));
        assertThat(DtoMapper.toAction(new ActionRequest.Move(0, 4, 1L))).isEqualTo(new Action.Move(0, 4));
        assertThat(DtoMapper.toAction(new ActionRequest.Merge(1, 0, 1L))).isEqualTo(new Action.Merge(1, 0));
    }
}
