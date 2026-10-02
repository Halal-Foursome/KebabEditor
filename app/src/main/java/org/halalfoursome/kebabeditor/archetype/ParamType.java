package org.halalfoursome.kebabeditor.archetype;

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
    NODE_REF;

    public String displayName() {
        return switch (this) {
            case INT        -> "Int";
            case FLOAT      -> "Float";
            case BOOL       -> "Boolean";
            case STRING     -> "String";
            case VEC3       -> "Vec3";
            case NODE_REF   -> "NodeRef";
            case REL_PATH   -> "RelPath";
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
            case INT        -> Integer.class;
            case FLOAT      -> Float.class;
            case BOOL       -> Boolean.class;
            case STRING     -> String.class;
            case REL_PATH   -> String.class;
            case VEC3       -> Vec3.class;
            case NODE_REF   -> Maybe.class; // Maybe<String>
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
        };
    }
}
