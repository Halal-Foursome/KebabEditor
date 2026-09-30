package org.halalfoursome.kebabeditor.archetype;

import java.util.Optional;

import org.halalfoursome.kebabeditor.math.Vec3;

public enum ParamType {
    INT,
    FLOAT,
    BOOL,
    STRING,
    VEC3,
    NODE_REF;

    public Object defaultValue() {
        return switch (this) {
            case INT        -> 0;
            case FLOAT      -> 0.0f;
            case BOOL       -> false;
            case STRING     -> "";
            case VEC3       -> new Vec3();
            case NODE_REF   -> Optional.empty();
        };
    }
}
