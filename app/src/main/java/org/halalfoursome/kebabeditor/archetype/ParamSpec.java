package org.halalfoursome.kebabeditor.archetype;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = ParamSpecJson.Serializer.class)
@JsonDeserialize(using = ParamSpecJson.Deserializer.class)
public record ParamSpec(ParamType type, Object defaultValue) {

    public ParamSpec {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(defaultValue, "defaultValue");

        if (!type.javaClass().isInstance(defaultValue)) {
            throw new IllegalArgumentException(
                type.displayName() + " parameter cannot default to a "
                    + defaultValue.getClass().getSimpleName()
            );
        }
        if (type == ParamType.NODE_REF && ((Maybe<?>) defaultValue).isSome()) {
            throw new IllegalArgumentException("A node reference cannot have a default, nodes belong to one scene");
        }

        defaultValue = copyOf(defaultValue);
    }

    public static ParamSpec of(ParamType type) {
        return new ParamSpec(type, type.defaultValue());
    }

    @Override
    public Object defaultValue() {
        return copyOf(defaultValue);
    }

    public boolean usesTypeDefault() {
        return defaultValue.equals(type.defaultValue());
    }

    private static Object copyOf(Object value) {
        return value instanceof Vec3 vec ? vec.copy() : value;
    }
}
