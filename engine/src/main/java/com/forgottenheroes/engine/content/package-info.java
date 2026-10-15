/**
 * Heroes, tokens and relics described as data. The JSON files under {@code content/} are read by
 * {@code ContentLoader}, checked against {@code content.schema.json}, and served through
 * {@code ContentIndex}. The ability DSL (trigger, targets, effects, amounts) lives here as records;
 * the interpreter that runs it arrives in Step 16.
 */
package com.forgottenheroes.engine.content;
