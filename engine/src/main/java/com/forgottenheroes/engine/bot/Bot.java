package com.forgottenheroes.engine.bot;

import com.forgottenheroes.engine.action.Action;
import com.forgottenheroes.engine.content.ContentIndex;
import com.forgottenheroes.engine.model.RunState;
import com.forgottenheroes.engine.rng.GameRandom;

/**
 * A player made of code: given the state, it names the next action, {@code EndTurn} included. It
 * may propose something the rules refuse; the runner then asks again, and counts the refusal, because
 * a bot that keeps proposing illegal moves points at a confusing rule.
 */
public interface Bot {

    String name();

    Action next(RunState state, ContentIndex content, GameRandom rng);
}
