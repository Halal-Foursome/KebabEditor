package org.halalfoursome.kebabeditor.archetype.instance;

import org.halalfoursome.kebabeditor.archetype.ParamType;
import org.halalfoursome.kebabeditor.archetype.error.ParamDecodeException;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

public final class ParamJson {

    private ParamJson() {}

    public static JsonNode encode(ParamType type, Object value) {
        return switch (type) {
            case INT                -> IntNode.valueOf((Integer) value);
            case FLOAT              -> FloatNode.valueOf((Float) value);
            case BOOL               -> BooleanNode.valueOf((Boolean) value);
            case STRING, REL_PATH   -> TextNode.valueOf((String) value);
            case VEC3               -> TextNode.valueOf(value.toString());
            case NODE_REF           -> ((Maybe<?>) value).<JsonNode>mapOr(
                NullNode.getInstance(),
                id -> TextNode.valueOf((String) id)
            );
        };
    }

    public static Object decode(ParamType type, JsonNode node) throws ParamDecodeException {
        return switch (type) {
            case INT -> node.isIntegralNumber() && node.canConvertToInt()
                ? (Object) node.intValue()
                : fail(type, node);

            case FLOAT -> node.isNumber()
                ? (Object) node.floatValue()
                : fail(type, node);

            case BOOL -> node.isBoolean()
                ? (Object) node.booleanValue()
                : fail(type, node);

            case STRING, REL_PATH -> node.isTextual()
                ? (Object) node.textValue()
                : fail(type, node);
                
            case VEC3 -> decodeVec3(node);
            case NODE_REF -> node.isNull()
                ? Maybe.none()
                : node.isTextual()
                    ? Maybe.some(node.textValue())
                    : fail(type, node);
        };
    }

    private static Vec3 decodeVec3(JsonNode node) throws ParamDecodeException {
        if (node.isTextual()) {
            Maybe<Vec3> parsed = Vec3.fromString(node.textValue());

            if (parsed.isSome()) {
                return parsed.unwrap();
            }
        }

        return fail(ParamType.VEC3, node);
    }

    private static <T> T fail(ParamType type, JsonNode node) throws ParamDecodeException {
        throw new ParamDecodeException(type, node);
    }
}
