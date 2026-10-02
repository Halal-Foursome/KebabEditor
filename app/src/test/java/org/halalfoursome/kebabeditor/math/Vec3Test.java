package org.halalfoursome.kebabeditor.math;

import static org.halalfoursome.kebabeditor.math.MathAssert.EPS;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class Vec3Test {

    @Test
    void arithmetic() {
        Vec3 a = new Vec3(1, 2, 3);
        Vec3 b = new Vec3(4, 5, 6);
        assertVec(new Vec3(5, 7, 9), a.add(b));
        assertVec(new Vec3(-3, -3, -3), a.sub(b));
        assertVec(new Vec3(2, 4, 6), a.mul(2));
        assertVec(new Vec3(4, 10, 18), a.mul(b));
        assertVec(new Vec3(0.5f, 1, 1.5f), a.div(2));
        assertVec(new Vec3(-1, -2, -3), a.negate());
    }

    @Test
    void allocatingOpsDoNotMutate() {
        Vec3 a = new Vec3(1, 2, 3);
        a.add(new Vec3(1, 1, 1));
        a.normalize();
        assertEquals(new Vec3(1, 2, 3), a);
    }

    @Test
    void localOpsMutateAndReturnThis() {
        Vec3 a = new Vec3(1, 2, 3);
        assertSame(a, a.addLocal(new Vec3(1, 1, 1)));
        assertVec(new Vec3(2, 3, 4), a);
        a.mulLocal(2).subLocal(new Vec3(1, 1, 1));
        assertVec(new Vec3(3, 5, 7), a);
    }

    @Test
    void dotAndCross() {
        assertEquals(32f, new Vec3(1, 2, 3).dot(new Vec3(4, 5, 6)), EPS);
        assertVec(Vec3.unitZ(), Vec3.unitX().cross(Vec3.unitY()));
        assertVec(new Vec3(-3, 6, -3), new Vec3(1, 2, 3).cross(new Vec3(4, 5, 6)));
    }

    @Test
    void crossLocalWithSelfIsZero() {
        Vec3 a = new Vec3(1, 2, 3);
        assertVec(Vec3.zero(), a.crossLocal(a));
    }

    @Test
    void lengthAndNormalize() {
        Vec3 v = new Vec3(3, 4, 0);
        assertEquals(5f, v.length(), EPS);
        assertEquals(25f, v.lengthSquared(), EPS);
        assertEquals(1f, v.normalize().length(), EPS);
        assertVec(Vec3.zero(), Vec3.zero().normalize());
        assertEquals(5f, v.distance(Vec3.zero()), EPS);
        assertEquals(25f, v.distanceSquared(Vec3.zero()), EPS);
    }

    @Test
    void lerpMinMaxAbs() {
        assertVec(new Vec3(5, 5, 5), Vec3.zero().lerp(new Vec3(10, 10, 10), 0.5f));
        assertVec(new Vec3(1, 2, 3), new Vec3(1, 5, 3).min(new Vec3(4, 2, 6)));
        assertVec(new Vec3(4, 5, 6), new Vec3(1, 5, 3).max(new Vec3(4, 2, 6)));
        assertVec(new Vec3(1, 2, 3), new Vec3(-1, 2, -3).abs());
    }

    @Test
    void reflectAndProject() {
        assertVec(new Vec3(1, 1, 0), new Vec3(1, -1, 0).reflect(Vec3.unitY()));
        assertVec(new Vec3(3, 0, 0), new Vec3(3, 4, 0).project(Vec3.unitX()));
    }

    @Test
    void angle() {
        assertEquals(Math.PI / 2, Vec3.unitX().angle(Vec3.unitY()), EPS);
        assertEquals(0f, Vec3.unitX().angle(Vec3.unitX()), EPS);
        assertEquals(Math.PI, Vec3.unitX().angle(Vec3.unitX().negate()), EPS);
    }
}
