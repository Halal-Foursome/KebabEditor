package org.halalfoursome.kebabeditor.math;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mat4 {

    // Column 0
    private float m00;
    private float m10;
    private float m20;
    private float m30;

    // Column 1
    private float m01;
    private float m11;
    private float m21;
    private float m31;

    // Column 2
    private float m02;
    private float m12;
    private float m22;
    private float m32;

    // Column 3
    private float m03;
    private float m13;
    private float m23;
    private float m33;

    public static Mat4 identity() {
        return new Mat4().setIdentity();
    }

    public static Mat4 fromArray(float[] a) {
        return new Mat4().set(a);
    }

    public static Mat4 translation(Vec3 t) {
        return new Mat4().setIdentity().translateLocal(t);
    }

    public static Mat4 scaling(Vec3 s) {
        return new Mat4().setIdentity().scaleLocal(s);
    }

    public static Mat4 scaling(float s) {
        return scaling(new Vec3(s, s, s));
    }

    public static Mat4 rotation(Quat q) {
        return new Mat4().setRotation(q);
    }

    public static Mat4 compose(Vec3 translation, Quat rotation, Vec3 scale) {
        return new Mat4().setCompose(translation, rotation, scale.getX(), scale.getY(), scale.getZ());
    }

    public static Mat4 perspective(float fovYRadians, float aspect, float near, float far) {
        float f = 1f / (float) Math.tan(fovYRadians * 0.5f);
        return new Mat4(
            f / aspect, 0, 0, 0,
            0, f, 0, 0,
            0, 0, (far + near) / (near - far), -1,
            0, 0, (2f * far * near) / (near - far), 0
        );
    }

    public static Mat4 orthographic(float left, float right, float bottom, float top, float near, float far) {
        return new Mat4(
            2f / (right - left), 0, 0, 0,
            0, 2f / (top - bottom), 0, 0,
            0, 0, -2f / (far - near), 0,
            -(right + left) / (right - left), -(top + bottom) / (top - bottom), -(far + near) / (far - near), 1
        );
    }

    public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
        Vec3 f = target.sub(eye).normalizeLocal();
        Vec3 s = f.cross(up).normalizeLocal();
        Vec3 u = s.cross(f);
        return new Mat4(
            s.getX(), u.getX(), -f.getX(), 0,
            s.getY(), u.getY(), -f.getY(), 0,
            s.getZ(), u.getZ(), -f.getZ(), 0,
            -s.dot(eye), -u.dot(eye), f.dot(eye), 1
        );
    }

    public Mat4 copy() {
        return new Mat4(
            m00, m10, m20, m30,
            m01, m11, m21, m31,
            m02, m12, m22, m32,
            m03, m13, m23, m33
        );
    }

    public Mat4 set(Mat4 o) {
        return set(
            o.m00, o.m10, o.m20, o.m30,
            o.m01, o.m11, o.m21, o.m31,
            o.m02, o.m12, o.m22, o.m32,
            o.m03, o.m13, o.m23, o.m33
        );
    }

    public Mat4 set(float[] a) {
        return set(
            a[0], a[1], a[2], a[3],
            a[4], a[5], a[6], a[7],
            a[8], a[9], a[10], a[11],
            a[12], a[13], a[14], a[15]
        );
    }

    public Mat4 set(
        float m00, float m10, float m20, float m30,
        float m01, float m11, float m21, float m31,
        float m02, float m12, float m22, float m32,
        float m03, float m13, float m23, float m33
    ) {
        this.m00 = m00; this.m10 = m10; this.m20 = m20; this.m30 = m30;
        this.m01 = m01; this.m11 = m11; this.m21 = m21; this.m31 = m31;
        this.m02 = m02; this.m12 = m12; this.m22 = m22; this.m32 = m32;
        this.m03 = m03; this.m13 = m13; this.m23 = m23; this.m33 = m33;
        return this;
    }

    public Mat4 setIdentity() {
        return set(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        );
    }

    public Mat4 setRotation(Quat q) {
        float x = q.getX(), y = q.getY(), z = q.getZ(), w = q.getW();
        float xx = x * x, yy = y * y, zz = z * z;
        float xy = x * y, xz = x * z, yz = y * z;
        float wx = w * x, wy = w * y, wz = w * z;
        return set(
            1 - 2 * (yy + zz), 2 * (xy + wz),     2 * (xz - wy),     0,
            2 * (xy - wz),     1 - 2 * (xx + zz), 2 * (yz + wx),     0,
            2 * (xz + wy),     2 * (yz - wx),     1 - 2 * (xx + yy), 0,
            0,                 0,                 0,                 1
        );
    }

    public Mat4 setCompose(Vec3 t, Quat q, float sx, float sy, float sz) {
        setRotation(q);
        m00 *= sx; m10 *= sx; m20 *= sx;
        m01 *= sy; m11 *= sy; m21 *= sy;
        m02 *= sz; m12 *= sz; m22 *= sz;
        m03 = t.getX();
        m13 = t.getY();
        m23 = t.getZ();
        return this;
    }

    public Mat4 setCompose(Vec3 t, Quat q, float uniformScale) {
        return setCompose(t, q, uniformScale, uniformScale, uniformScale);
    }

    public Mat4 mulLocal(Mat4 o) {
        float n00 = m00 * o.m00 + m01 * o.m10 + m02 * o.m20 + m03 * o.m30;
        float n10 = m10 * o.m00 + m11 * o.m10 + m12 * o.m20 + m13 * o.m30;
        float n20 = m20 * o.m00 + m21 * o.m10 + m22 * o.m20 + m23 * o.m30;
        float n30 = m30 * o.m00 + m31 * o.m10 + m32 * o.m20 + m33 * o.m30;

        float n01 = m00 * o.m01 + m01 * o.m11 + m02 * o.m21 + m03 * o.m31;
        float n11 = m10 * o.m01 + m11 * o.m11 + m12 * o.m21 + m13 * o.m31;
        float n21 = m20 * o.m01 + m21 * o.m11 + m22 * o.m21 + m23 * o.m31;
        float n31 = m30 * o.m01 + m31 * o.m11 + m32 * o.m21 + m33 * o.m31;

        float n02 = m00 * o.m02 + m01 * o.m12 + m02 * o.m22 + m03 * o.m32;
        float n12 = m10 * o.m02 + m11 * o.m12 + m12 * o.m22 + m13 * o.m32;
        float n22 = m20 * o.m02 + m21 * o.m12 + m22 * o.m22 + m23 * o.m32;
        float n32 = m30 * o.m02 + m31 * o.m12 + m32 * o.m22 + m33 * o.m32;

        float n03 = m00 * o.m03 + m01 * o.m13 + m02 * o.m23 + m03 * o.m33;
        float n13 = m10 * o.m03 + m11 * o.m13 + m12 * o.m23 + m13 * o.m33;
        float n23 = m20 * o.m03 + m21 * o.m13 + m22 * o.m23 + m23 * o.m33;
        float n33 = m30 * o.m03 + m31 * o.m13 + m32 * o.m23 + m33 * o.m33;

        return set(
            n00, n10, n20, n30,
            n01, n11, n21, n31,
            n02, n12, n22, n32,
            n03, n13, n23, n33
        );
    }

    public Mat4 transposeLocal() {
        return set(
            m00, m01, m02, m03,
            m10, m11, m12, m13,
            m20, m21, m22, m23,
            m30, m31, m32, m33
        );
    }

    public Mat4 inverseLocal() {
        float s0 = m00 * m11 - m10 * m01;
        float s1 = m00 * m21 - m20 * m01;
        float s2 = m00 * m31 - m30 * m01;
        float s3 = m10 * m21 - m20 * m11;
        float s4 = m10 * m31 - m30 * m11;
        float s5 = m20 * m31 - m30 * m21;

        float c5 = m22 * m33 - m32 * m23;
        float c4 = m12 * m33 - m32 * m13;
        float c3 = m12 * m23 - m22 * m13;
        float c2 = m02 * m33 - m32 * m03;
        float c1 = m02 * m23 - m22 * m03;
        float c0 = m02 * m13 - m12 * m03;

        float det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
        if (det == 0f) {
            throw new ArithmeticException("Matrix is singular");
        }
        float inv = 1f / det;

        return set(
            ( m11 * c5 - m21 * c4 + m31 * c3) * inv,
            (-m10 * c5 + m20 * c4 - m30 * c3) * inv,
            ( m13 * s5 - m23 * s4 + m33 * s3) * inv,
            (-m12 * s5 + m22 * s4 - m32 * s3) * inv,

            (-m01 * c5 + m21 * c2 - m31 * c1) * inv,
            ( m00 * c5 - m20 * c2 + m30 * c1) * inv,
            (-m03 * s5 + m23 * s2 - m33 * s1) * inv,
            ( m02 * s5 - m22 * s2 + m32 * s1) * inv,

            ( m01 * c4 - m11 * c2 + m31 * c0) * inv,
            (-m00 * c4 + m10 * c2 - m30 * c0) * inv,
            ( m03 * s4 - m13 * s2 + m33 * s0) * inv,
            (-m02 * s4 + m12 * s2 - m32 * s0) * inv,

            (-m01 * c3 + m11 * c1 - m21 * c0) * inv,
            ( m00 * c3 - m10 * c1 + m20 * c0) * inv,
            (-m03 * s3 + m13 * s1 - m23 * s0) * inv,
            ( m02 * s3 - m12 * s1 + m22 * s0) * inv
        );
    }

    public Mat4 translateLocal(Vec3 t) {
        float tx = t.getX(), ty = t.getY(), tz = t.getZ();
        m03 += m00 * tx + m01 * ty + m02 * tz;
        m13 += m10 * tx + m11 * ty + m12 * tz;
        m23 += m20 * tx + m21 * ty + m22 * tz;
        m33 += m30 * tx + m31 * ty + m32 * tz;
        return this;
    }

    public Mat4 scaleLocal(Vec3 s) {
        float sx = s.getX(), sy = s.getY(), sz = s.getZ();
        m00 *= sx; m10 *= sx; m20 *= sx; m30 *= sx;
        m01 *= sy; m11 *= sy; m21 *= sy; m31 *= sy;
        m02 *= sz; m12 *= sz; m22 *= sz; m32 *= sz;
        return this;
    }

    public Vec3 transformPointLocal(Vec3 p) {
        float px = p.getX(), py = p.getY(), pz = p.getZ();
        float x = m00 * px + m01 * py + m02 * pz + m03;
        float y = m10 * px + m11 * py + m12 * pz + m13;
        float z = m20 * px + m21 * py + m22 * pz + m23;
        float w = m30 * px + m31 * py + m32 * pz + m33;
        return w == 1f || w == 0f ? p.set(x, y, z) : p.set(x / w, y / w, z / w);
    }

    public Vec3 transformDirectionLocal(Vec3 d) {
        float dx = d.getX(), dy = d.getY(), dz = d.getZ();
        return d.set(
            m00 * dx + m01 * dy + m02 * dz,
            m10 * dx + m11 * dy + m12 * dz,
            m20 * dx + m21 * dy + m22 * dz
        );
    }

    public Vec3 getTranslation(Vec3 dest) {
        return dest.set(m03, m13, m23);
    }

    public float[] toArray(float[] dest) {
        dest[0] = m00; dest[1] = m10; dest[2] = m20; dest[3] = m30;
        dest[4] = m01; dest[5] = m11; dest[6] = m21; dest[7] = m31;
        dest[8] = m02; dest[9] = m12; dest[10] = m22; dest[11] = m32;
        dest[12] = m03; dest[13] = m13; dest[14] = m23; dest[15] = m33;
        return dest;
    }

    public float[] toArray() {
        return toArray(new float[16]);
    }

    public Mat4 mul(Mat4 o) {
        return copy().mulLocal(o);
    }

    public Mat4 transpose() {
        return copy().transposeLocal();
    }

    public Mat4 inverse() {
        return copy().inverseLocal();
    }

    public Vec3 transformPoint(Vec3 p) {
        return transformPointLocal(p.copy());
    }

    public Vec3 transformDirection(Vec3 d) {
        return transformDirectionLocal(d.copy());
    }

    public Vec3 getTranslation() {
        return getTranslation(new Vec3());
    }

    public float determinant() {
        float s0 = m00 * m11 - m10 * m01;
        float s1 = m00 * m21 - m20 * m01;
        float s2 = m00 * m31 - m30 * m01;
        float s3 = m10 * m21 - m20 * m11;
        float s4 = m10 * m31 - m30 * m11;
        float s5 = m20 * m31 - m30 * m21;

        float c5 = m22 * m33 - m32 * m23;
        float c4 = m12 * m33 - m32 * m13;
        float c3 = m12 * m23 - m22 * m13;
        float c2 = m02 * m33 - m32 * m03;
        float c1 = m02 * m23 - m22 * m03;
        float c0 = m02 * m13 - m12 * m03;

        return s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
    }
}
