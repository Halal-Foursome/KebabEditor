package org.halalfoursome.kebabeditor.math;

import static org.halalfoursome.kebabeditor.math.MathAssert.EPS;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertMat;
import static org.halalfoursome.kebabeditor.math.MathAssert.assertVec;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Mat4Test {

    private static Mat4 sample() {
        Quat q = Quat.fromAxisAngle(new Vec3(1, 2, 3), 0.8f);
        return Mat4.compose(new Vec3(4, -5, 6), q, new Vec3(2, 3, 0.5f));
    }

    @Test
    void identityLeavesPointsUnchanged() {
        assertVec(new Vec3(1, 2, 3), Mat4.identity().transformPoint(new Vec3(1, 2, 3)));
    }

    @Test
    void arrayIsColumnMajor() {
        float[] a = Mat4.translation(new Vec3(7, 8, 9)).toArray();
        assertArrayEquals(new float[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 7, 8, 9, 1 }, a);
        assertMat(Mat4.translation(new Vec3(7, 8, 9)), Mat4.fromArray(a));
    }

    @Test
    void translationAffectsPointsNotDirections() {
        Mat4 t = Mat4.translation(new Vec3(1, 2, 3));
        assertVec(new Vec3(2, 3, 4), t.transformPoint(Vec3.one()));
        assertVec(Vec3.one(), t.transformDirection(Vec3.one()));
        assertVec(new Vec3(1, 2, 3), t.getTranslation());
    }

    @Test
    void scaling() {
        assertVec(new Vec3(2, 6, 12), Mat4.scaling(new Vec3(2, 3, 4)).transformPoint(new Vec3(1, 2, 3)));
        assertVec(new Vec3(2, 4, 6), Mat4.scaling(2).transformPoint(new Vec3(1, 2, 3)));
    }

    @Test
    void multiplyAppliesRightOperandFirst() {
        Mat4 t = Mat4.translation(new Vec3(10, 0, 0));
        Mat4 s = Mat4.scaling(2);
        assertVec(new Vec3(12, 0, 0), t.mul(s).transformPoint(new Vec3(1, 0, 0)));
        assertVec(new Vec3(22, 0, 0), s.mul(t).transformPoint(new Vec3(1, 0, 0)));
    }

    @Test
    void multiplyByIdentity() {
        Mat4 m = sample();
        assertMat(m, m.mul(Mat4.identity()));
        assertMat(m, Mat4.identity().mul(m));
    }

    @Test
    void mulLocalWithSelfMatchesMul() {
        Mat4 m = sample();
        Mat4 expected = m.mul(m);
        assertSame(m, m.mulLocal(m));
        assertMat(expected, m);
    }

    @Test
    void multiplyIsAssociative() {
        Mat4 a = sample();
        Mat4 b = Mat4.translation(new Vec3(1, 2, 3));
        Mat4 c = Mat4.rotation(Quat.fromAxisAngle(Vec3.unitY(), 0.4f));
        assertMat(a.mul(b).mul(c), a.mul(b.mul(c)));
    }

    @Test
    void transpose() {
        Mat4 m = sample();
        assertMat(m, m.transpose().transpose());
        assertEquals(m.getM01(), m.transpose().getM10(), EPS);
        assertEquals(m.getM23(), m.transpose().getM32(), EPS);
    }

    @Test
    void determinant() {
        assertEquals(1f, Mat4.identity().determinant(), EPS);
        assertEquals(24f, Mat4.scaling(new Vec3(2, 3, 4)).determinant(), EPS);
        assertEquals(1f, Mat4.rotation(Quat.fromAxisAngle(Vec3.unitX(), 1f)).determinant(), EPS);
        assertEquals(0f, Mat4.scaling(new Vec3(1, 0, 1)).determinant(), EPS);
    }

    @Test
    void inverseTimesOriginalIsIdentity() {
        Mat4 m = sample();
        assertMat(Mat4.identity(), m.mul(m.inverse()));
        assertMat(Mat4.identity(), m.inverse().mul(m));
    }

    @Test
    void inverseOfGeneralMatrix() {
        Mat4 m = Mat4.perspective(1f, 1.5f, 0.1f, 100f).mul(sample());
        Mat4 inv = m.inverse();
        assertArrayEquals(Mat4.identity().toArray(), m.mul(inv).toArray(), 1e-3f);
    }

    @Test
    void inverseRestoresPoints() {
        Mat4 m = sample();
        Vec3 p = new Vec3(1, 2, 3);
        assertVec(p, m.inverse().transformPoint(m.transformPoint(p)));
    }

    @Test
    void singularInverseThrows() {
        assertThrows(ArithmeticException.class, () -> Mat4.scaling(new Vec3(1, 0, 1)).inverse());
    }

    @Test
    void translateAndScaleLocalPostMultiply() {
        Mat4 m = sample();
        assertMat(m.mul(Mat4.translation(new Vec3(1, 2, 3))), m.copy().translateLocal(new Vec3(1, 2, 3)));
        assertMat(m.mul(Mat4.scaling(new Vec3(1, 2, 3))), m.copy().scaleLocal(new Vec3(1, 2, 3)));
    }

    @Test
    void composeMatchesTrs() {
        Vec3 t = new Vec3(1, 2, 3);
        Quat q = Quat.fromAxisAngle(Vec3.unitZ(), 0.6f);
        Vec3 s = new Vec3(2, 2, 2);
        Mat4 expected = Mat4.translation(t).mul(Mat4.rotation(q)).mul(Mat4.scaling(s));
        assertMat(expected, Mat4.compose(t, q, s));
    }

    @Test
    void perspectiveMapsNearAndFarPlanes() {
        Mat4 p = Mat4.perspective((float) Math.toRadians(90), 1f, 1f, 10f);
        assertEquals(-1f, p.transformPoint(new Vec3(0, 0, -1)).getZ(), EPS);
        assertEquals(1f, p.transformPoint(new Vec3(0, 0, -10)).getZ(), EPS);
        assertVec(new Vec3(1, 1, -1), p.transformPoint(new Vec3(1, 1, -1)));
    }

    @Test
    void orthographicMapsBoxToNdc() {
        Mat4 o = Mat4.orthographic(-2, 2, -1, 1, 1, 5);
        assertVec(new Vec3(1, 1, -1), o.transformPoint(new Vec3(2, 1, -1)));
        assertVec(new Vec3(-1, -1, 1), o.transformPoint(new Vec3(-2, -1, -5)));
    }

    @Test
    void lookAtPlacesTargetInFrontOfCamera() {
        Mat4 v = Mat4.lookAt(new Vec3(0, 0, 5), Vec3.zero(), Vec3.unitY());
        assertVec(new Vec3(0, 0, -5), v.transformPoint(Vec3.zero()));
        assertVec(Vec3.zero(), v.transformPoint(new Vec3(0, 0, 5)));
        assertVec(Vec3.unitX(), v.transformDirection(Vec3.unitX()));
    }

    @Test
    void localTransformsMutateVector() {
        Vec3 p = new Vec3(1, 2, 3);
        assertSame(p, Mat4.translation(Vec3.one()).transformPointLocal(p));
        assertVec(new Vec3(2, 3, 4), p);
    }

    @Test
    void toArrayWritesIntoBuffer() {
        float[] buf = new float[16];
        assertSame(buf, sample().toArray(buf));
        assertArrayEquals(sample().toArray(), buf, 0f);
    }
}
