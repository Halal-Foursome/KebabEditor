package org.halalfoursome.kebabeditor.archetype;

import java.io.IOException;

import org.halalfoursome.kebabeditor.archetype.error.ParamDecodeException;
import org.halalfoursome.kebabeditor.archetype.instance.ParamJson;
import org.halalfoursome.kebabeditor.logging.LogService;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public final class ParamSpecJson {

    private static final String TYPE = "type";
    private static final String DEFAULT = "default";

    private ParamSpecJson() {}

    public static final class Serializer extends StdSerializer<ParamSpec> {

        public Serializer() {
            super(ParamSpec.class);
        }

        @Override
        public void serialize(ParamSpec spec, JsonGenerator generator, SerializerProvider provider) throws IOException {
            if (spec.usesTypeDefault()) {
                generator.writeString(spec.type().jsonName());

                return;
            }

            generator.writeStartObject();
            generator.writeStringField(TYPE, spec.type().jsonName());
            generator.writeFieldName(DEFAULT);
            generator.writeTree(ParamJson.encode(spec.type(), spec.defaultValue()));
            generator.writeEndObject();
        }
    }

    public static final class Deserializer extends StdDeserializer<ParamSpec> {

        public Deserializer() {
            super(ParamSpec.class);
        }

        @Override
        public ParamSpec deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String name = parser.currentName();
            JsonNode node = context.readTree(parser);
            ParamType type = typeOf(parser, name, node.isObject() ? node.path(TYPE) : node);

            if (!node.isObject() || !node.has(DEFAULT)) {
                return ParamSpec.of(type);
            }

            try {
                return new ParamSpec(type, ParamJson.decode(type, node.get(DEFAULT)));
            } catch (ParamDecodeException | IllegalArgumentException e) {
                LogService.get().warn(
                    "Ignoring the default of parameter '" + name + "' (" + e.getMessage()
                        + "), using the " + type.displayName() + " default"
                );

                return ParamSpec.of(type);
            }
        }

        private static ParamType typeOf(JsonParser parser, String name, JsonNode node) throws JsonMappingException {
            if (!node.isTextual()) {
                throw JsonMappingException.from(parser, "Parameter '" + name + "' needs a type");
            }

            try {
                return ParamType.fromJson(node.textValue());
            } catch (IllegalArgumentException e) {
                throw JsonMappingException.from(
                    parser,
                    "Parameter '" + name + "' has an unknown type '" + node.textValue() + "'"
                );
            }
        }
    }
}
