package org.halalfoursome.kebabeditor.math;

import static org.halalfoursome.kebabeditor.math.MathAssert.EPS;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertMat;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertVec;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class QuatTest {

    private static final float HALF_PI = (float) (Math.PI / 2);

    @Test
    void identityDoesNotRotate() {
        assertVec(new Vec3(1, 2, 3), Quat.identity().rotate(new Vec3(1, 2, 3)));
    }

    @Test
    void axisAngleRotatesRightHanded() {
        Quat q = Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI);
        assertVec(Vec3.unitY(), q.rotate(Vec3.unitX()));
        assertVec(Vec3.unitZ(), q.rotate(Vec3.unitZ()));
    }

    @Test
    void rotateLocalMutatesVector() {
        Quat q = Quat.fromAxisAngle(Vec3.unitY(), HALF_PI);
        Vec3 v = Vec3.unitX();
        assertSame(v, q.rotateLocal(v));
        assertVec(new Vec3(0, 0, -1), v);
    }

    @Test
    void multiplyComposesRotations() {
        Quat a = Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI);
        Quat b = Quat.fromAxisAngle(Vec3.unitX(), HALF_PI);
        Vec3 v = new Vec3(1, 2, 3);
        assertVec(a.rotate(b.rotate(v)), a.mul(b).rotate(v));
    }

    @Test
    void mulLocalWithSelf() {
        Quat q = Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI);
        q.mulLocal(q);
        assertVec(new Vec3(-1, 0, 0), q.rotate(Vec3.unitX()));
    }

    @Test
    void inverseUndoesRotation() {
        Quat q = Quat.fromAxisAngle(new Vec3(1, 1, 0), 1.2f);
        Vec3 v = new Vec3(1, 2, 3);
        assertVec(v, q.inverse().rotate(q.rotate(v)));
        Quat id = q.mul(q.inverse());
        assertEquals(1f, id.getW(), EPS);
    }

    @Test
    void conjugateOfUnitEqualsInverse() {
        Quat q = Quat.fromAxisAngle(Vec3.unitY(), 0.7f);
        Quat c = q.conjugate();
        Quat i = q.inverse();
        assertEquals(c.getX(), i.getX(), EPS);
        assertEquals(c.getY(), i.getY(), EPS);
        assertEquals(c.getZ(), i.getZ(), EPS);
        assertEquals(c.getW(), i.getW(), EPS);
    }

    @Test
    void normalize() {
        assertEquals(1f, new Quat(1, 2, 3, 4).normalize().length(), EPS);
        assertEquals(1f, new Quat(0, 0, 0, 0).normalize().getW(), EPS);
    }

    @Test
    void slerpEndpointsAndMidpoint() {
        Quat a = Quat.identity();
        Quat b = Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI);
        assertVec(Vec3.unitX(), a.slerp(b, 0).rotate(Vec3.unitX()));
        assertVec(Vec3.unitY(), a.slerp(b, 1).rotate(Vec3.unitX()));
        Vec3 mid = a.slerp(b, 0.5f).rotate(Vec3.unitX());
        float s = (float) Math.sqrt(0.5);
        assertVec(new Vec3(s, s, 0), mid);
    }

    @Test
    void slerpTakesShortestPath() {
        Quat a = Quat.identity();
        Quat b = Quat.fromAxisAngle(Vec3.unitZ(), HALF_PI);
        Quat negB = new Quat(-b.getX(), -b.getY(), -b.getZ(), -b.getW());
        assertVec(Vec3.unitY(), a.slerp(negB, 1).rotate(Vec3.unitX()));
    }

    @Test
    void fromToRotatesFirstOntoSecond() {
        Vec3 from = new Vec3(1, 0, 0);
        Vec3 to = new Vec3(0, 1, 1);
        assertVec(to.normalize(), Quat.fromTo(from, to).rotate(from));
        assertVec(from.negate(), Quat.fromTo(from, from.negate()).rotate(from));
    }

    @Test
    void toMat4MatchesRotate() {
        Quat q = Quat.fromAxisAngle(new Vec3(1, 2, 3), 0.9f);
        Vec3 v = new Vec3(0.3f, -1, 2);
        assertVec(q.rotate(v), q.toMat4().transformDirection(v));
        assertMat(Mat4.identity(), Quat.identity().toMat4());
    }

    @Test
    void eulerSingleAxes() {
        assertVec(Vec3.unitY(), Quat.fromEuler(0, 0, HALF_PI).rotate(Vec3.unitX()));
        assertVec(Vec3.unitZ(), Quat.fromEuler(HALF_PI, 0, 0).rotate(Vec3.unitY()));
        assertVec(new Vec3(0, 0, -1), Quat.fromEuler(0, HALF_PI, 0).rotate(Vec3.unitX()));
    }
}
