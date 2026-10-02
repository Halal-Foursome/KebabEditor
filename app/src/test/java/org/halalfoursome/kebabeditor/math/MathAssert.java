package org.halalfoursome.kebabeditor.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MathAssert {

    static final float EPS = 1e-4f;

    private MathAssert() {}

    static void assertVec(Vec3 expected, Vec3 actual) {
        assertTrue(expected.approxEquals(actual, EPS), "expected " + expected + " but was " + actual);
    }

    static void assertMat(Mat4 expected, Mat4 actual) {
        assertArrayEquals(expected.toArray(), actual.toArray(), EPS);
    }
}
