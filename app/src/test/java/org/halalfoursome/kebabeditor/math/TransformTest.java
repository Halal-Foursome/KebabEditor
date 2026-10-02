package org.halalfoursome.kebabeditor.math;

import static org.halalfoursome.kebabeditor.math.MathAssert.EPS;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertMat;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class TransformTest {

    private static final float HALF_PI = (float) (Math.PI / 2);

    private static Transform a() {
        return new Transform(new Vec3(1, 2, 3), Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI), 2f);
    }

    private static Transform b() {
        return new Transform(new Vec3(-4, 0, 5), Quat.fromAxisAngle(new Vec3(1, 1, 0), 0.5f), 0.5f);
    }

    @Test
    void identityApply() {
        assertVec(new Vec3(1, 2, 3), Transform.identity().apply(new Vec3(1, 2, 3)));
    }

    @Test
    void applyScalesRotatesThenTranslates() {
        assertVec(new Vec3(1, 4, 3), a().apply(Vec3.unitX()));
    }

    @Test
    void applyMatchesMatrix() {
        Vec3 p = new Vec3(0.5f, -2, 7);
        assertVec(a().toMat4().transformPoint(p), a().apply(p));
    }

    @Test
    void mulMatchesMatrixProduct() {
        assertMat(a().toMat4().mul(b().toMat4()), a().mul(b()).toMat4());
    }

    @Test
    void mulComposesApply() {
        Vec3 p = new Vec3(1, 2, 3);
        assertVec(a().apply(b().apply(p)), a().mul(b()).apply(p));
    }

    @Test
    void mulLocalWithSelf() {
        Transform t = a();
        Transform expected = a().mul(a());
        t.mulLocal(t);
        assertMat(expected.toMat4(), t.toMat4());
    }

    @Test
    void inverseUndoesTransform() {
        Vec3 p = new Vec3(3, -1, 2);
        assertVec(p, a().inverse().apply(a().apply(p)));
        assertMat(Mat4.identity(), a().mul(a().inverse()).toMat4());
        assertMat(a().toMat4().inverse(), a().inverse().toMat4());
    }

    @Test
    void lerpEndpoints() {
        assertMat(a().toMat4(), a().lerp(b(), 0).toMat4());
        assertMat(b().toMat4(), a().lerp(b(), 1).toMat4());
        assertEquals(1.25f, a().lerp(b(), 0.5f).getScale(), EPS);
    }

    @Test
    void copyIsDeep() {
        Transform t = a();
        Transform c = t.copy();
        assertNotSame(t.getTranslation(), c.getTranslation());
        c.getTranslation().setX(100);
        assertEquals(1f, t.getTranslation().getX(), EPS);
    }

    @Test
    void setIdentity() {
        assertMat(Mat4.identity(), a().setIdentity().toMat4());
    }
}
