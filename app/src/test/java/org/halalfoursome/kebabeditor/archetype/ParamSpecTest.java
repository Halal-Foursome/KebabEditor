package org.halalfoursome.kebabeditor.archetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class ParamSpecTest {

    @Test
    void aDefaultOfTheWrongTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ParamSpec(ParamType.INT, "three"));
        assertThrows(IllegalArgumentException.class, () -> new ParamSpec(ParamType.FLOAT, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParamSpec(ParamType.VEC3, "(1, 2, 3)"));
    }

    @Test
    void nodeReferencesCannotHaveADefault() {
        assertThrows(IllegalArgumentException.class, () -> new ParamSpec(ParamType.NODE_REF, Maybe.some("node")));
        assertTrue(ParamSpec.of(ParamType.NODE_REF).usesTypeDefault());
    }

    @Test
    void mutatingAVec3DefaultNeverChangesTheSpec() {
        Vec3 original = new Vec3(1, 2, 3);
        ParamSpec spec = new ParamSpec(ParamType.VEC3, original);

        original.setX(99);
        ((Vec3) spec.defaultValue()).setY(99);

        assertEquals(new Vec3(1, 2, 3), spec.defaultValue());
        assertNotSame(spec.defaultValue(), spec.defaultValue());
    }
}
