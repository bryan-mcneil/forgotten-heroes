/**
 * Decision logic. A resolver validates an action against the rules (throwing {@code RuleViolation}
 * before anything changes) and then applies it by calling mutators, which are the only code that
 * writes to the state and which emit an event for every change.
 */
package com.forgottenheroes.engine.resolver;
