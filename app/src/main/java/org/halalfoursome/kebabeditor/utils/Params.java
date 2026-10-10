package org.halalfoursome.kebabeditor.utils;

import org.halalfoursome.kebabeditor.archetype.error.UnknownParamException;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Vec3;

public final class Params {

    private Params() {}

    public static Maybe<Vec3> vec3(ArchetypeInstance archetype, String name) {
        try {
            return archetype.get(name) instanceof Vec3 value ? Maybe.some(value) : Maybe.none();
        } catch (UnknownParamException e) {
            return Maybe.none();
        }
    }

    public static Maybe<Float> number(ArchetypeInstance archetype, String name) {
        try {
            return archetype.get(name) instanceof Float value ? Maybe.some(value) : Maybe.none();
        } catch (UnknownParamException e) {
            return Maybe.none();
        }
    }

    public static boolean positive(float value) {
        return Float.isFinite(value) && value > 0f;
    }
}
