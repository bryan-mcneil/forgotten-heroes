package com.forgottenheroes.engine.json;

import com.forgottenheroes.engine.model.Position;
import com.forgottenheroes.engine.model.Rank;
import com.forgottenheroes.engine.model.UnitId;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * How the small model values look in JSON, shared by the content loader and the event mapper:
 * a rank is its number ({@code 3}), a unit id is its text ({@code "u7"}) and a position is
 * {@code "home:0"}. The model classes themselves know nothing about JSON.
 */
public final class EngineJsonModule extends SimpleModule {

    public EngineJsonModule() {
        super("forgotten-heroes");
        addSerializer(Rank.class, new RankToNumber());
        addDeserializer(Rank.class, new RankFromNumber());
        addSerializer(UnitId.class, new UnitIdToText());
        addDeserializer(UnitId.class, new UnitIdFromText());
        addSerializer(Position.class, new PositionToText());
        addDeserializer(Position.class, new PositionFromText());
    }

    private static final class RankToNumber extends ValueSerializer<Rank> {
        @Override
        public void serialize(Rank rank, JsonGenerator generator, SerializationContext context) {
            generator.writeNumber(rank.number());
        }
    }

    private static final class RankFromNumber extends ValueDeserializer<Rank> {
        @Override
        public Rank deserialize(JsonParser parser, DeserializationContext context) {
            return Rank.of(parser.getIntValue());
        }
    }

    private static final class UnitIdToText extends ValueSerializer<UnitId> {
        @Override
        public void serialize(UnitId id, JsonGenerator generator, SerializationContext context) {
            generator.writeString(id.value());
        }
    }

    private static final class UnitIdFromText extends ValueDeserializer<UnitId> {
        @Override
        public UnitId deserialize(JsonParser parser, DeserializationContext context) {
            return new UnitId(parser.getString());
        }
    }

    private static final class PositionToText extends ValueSerializer<Position> {
        @Override
        public void serialize(Position position, JsonGenerator generator, SerializationContext context) {
            generator.writeString(position.toString());
        }
    }

    private static final class PositionFromText extends ValueDeserializer<Position> {
        @Override
        public Position deserialize(JsonParser parser, DeserializationContext context) {
            return Position.parse(parser.getString());
        }
    }
}
