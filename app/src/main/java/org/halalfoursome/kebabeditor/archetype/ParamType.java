package org.halalfoursome.kebabeditor.archetype;

import java.awt.Color;
import java.util.Locale;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ParamType {
    INT,
    FLOAT,
    BOOL,
    STRING,
    REL_PATH,
    VEC3,
    NODE_REF,
    COLOR;

    public String displayName() {
        return switch (this) {
            case INT        -> "Int";
            case FLOAT      -> "Float";
            case BOOL       -> "Boolean";
            case STRING     -> "String";
            case VEC3       -> "Vec3";
            case NODE_REF   -> "NodeRef";
            case REL_PATH   -> "RelPath";
            case COLOR      -> "Color";
        };
    }

    @JsonValue
    public String jsonName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static ParamType fromJson(String value) {
        return ParamType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public Class<?> javaClass() {
        return switch (this) {
            case INT        -> Integer.class;   // json: int
            case FLOAT      -> Float.class;     // json: float
            case BOOL       -> Boolean.class;   // json: boolean
            case STRING     -> String.class;    // json: string
            case REL_PATH   -> String.class;    // json: string, validated
            case VEC3       -> Vec3.class;      // json: string, validated
            case NODE_REF   -> Maybe.class;     // Maybe<String>, json: @Nullable string
            case COLOR      -> Color.class;     // json: string like a Vec3 with components from 0 to 1
        };
    }

    public Object defaultValue() {
        return switch (this) {
            case INT        -> 0;
            case FLOAT      -> 0.0f;
            case BOOL       -> false;
            case STRING     -> "";
            case REL_PATH   -> "";
            case VEC3       -> new Vec3();
            case NODE_REF   -> Maybe.none(); // Maybe<String>
            case COLOR      -> Color.BLACK;
        };
    }
}
