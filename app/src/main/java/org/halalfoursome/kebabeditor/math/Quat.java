package org.halalfoursome.kebabeditor.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quat {

    private float x;
    private float y;
    private float z;
    private float w;

    public static Quat identity() {
        return new Quat(0, 0, 0, 1);
    }

    public static Quat fromAxisAngle(Vec3 axis, float radians) {
        Vec3 n = axis.normalize();
        float half = radians * 0.5f;
        float s = (float) Math.sin(half);
        return new Quat(n.getX() * s, n.getY() * s, n.getZ() * s, (float) Math.cos(half));
    }

    public static Quat fromEuler(float pitch, float yaw, float roll) {
        Quat qx = fromAxisAngle(Vec3.unitX(), pitch);
        Quat qy = fromAxisAngle(Vec3.unitY(), yaw);
        Quat qz = fromAxisAngle(Vec3.unitZ(), roll);
        return qy.mulLocal(qx).mulLocal(qz);
    }

    public static Quat fromTo(Vec3 from, Vec3 to) {
        Vec3 a = from.normalize();
        Vec3 b = to.normalize();
        float d = a.dot(b);
        if (d < -0.999999f) {
            Vec3 axis = Vec3.unitX().crossLocal(a);
            if (axis.lengthSquared() < 1e-8f) {
                axis = Vec3.unitY().crossLocal(a);
            }
            return fromAxisAngle(axis, (float) Math.PI);
        }
        Vec3 c = a.crossLocal(b);
        return new Quat(c.getX(), c.getY(), c.getZ(), 1f + d).normalizeLocal();
    }

    public Quat copy() {
        return new Quat(x, y, z, w);
    }

    public Quat set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Quat set(Quat o) {
        return set(o.x, o.y, o.z, o.w);
    }

    public Quat setIdentity() {
        return set(0, 0, 0, 1);
    }

    public Quat mulLocal(Quat o) {
        return set(
            w * o.x + x * o.w + y * o.z - z * o.y,
            w * o.y - x * o.z + y * o.w + z * o.x,
            w * o.z + x * o.y - y * o.x + z * o.w,
            w * o.w - x * o.x - y * o.y - z * o.z
        );
    }

    public Quat conjugateLocal() {
        return set(-x, -y, -z, w);
    }

    public Quat inverseLocal() {
        float l2 = lengthSquared();
        return set(-x / l2, -y / l2, -z / l2, w / l2);
    }

    public Quat normalizeLocal() {
        float len = length();
        return len == 0 ? setIdentity() : set(x / len, y / len, z / len, w / len);
    }

    public Quat slerpLocal(Quat o, float t) {
        float d = dot(o);
        float sign = 1f;
        if (d < 0f) {
            d = -d;
            sign = -1f;
        }
        float wa;
        float wb;
        if (d > 0.9995f) {
            wa = 1f - t;
            wb = t * sign;
            return set(
                x * wa + o.x * wb,
                y * wa + o.y * wb,
                z * wa + o.z * wb,
                w * wa + o.w * wb
            ).normalizeLocal();
        }
        float theta = (float) Math.acos(d);
        float sin = (float) Math.sin(theta);
        wa = (float) Math.sin((1f - t) * theta) / sin;
        wb = (float) Math.sin(t * theta) / sin * sign;
        return set(
            x * wa + o.x * wb,
            y * wa + o.y * wb,
            z * wa + o.z * wb,
            w * wa + o.w * wb
        );
    }

    public Vec3 rotateLocal(Vec3 v) {
        float tx = 2f * (y * v.getZ() - z * v.getY());
        float ty = 2f * (z * v.getX() - x * v.getZ());
        float tz = 2f * (x * v.getY() - y * v.getX());
        return v.addLocal(
            w * tx + (y * tz - z * ty),
            w * ty + (z * tx - x * tz),
            w * tz + (x * ty - y * tx)
        );
    }

    public Quat mul(Quat o) {
        return copy().mulLocal(o);
    }

    public Quat conjugate() {
        return copy().conjugateLocal();
    }

    public Quat inverse() {
        return copy().inverseLocal();
    }

    public Quat normalize() {
        return copy().normalizeLocal();
    }

    public Quat slerp(Quat o, float t) {
        return copy().slerpLocal(o, t);
    }

    public Vec3 rotate(Vec3 v) {
        return rotateLocal(v.copy());
    }

    public float dot(Quat o) {
        return x * o.x + y * o.y + z * o.z + w * o.w;
    }

    public float lengthSquared() {
        return dot(this);
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Mat4 toMat4() {
        return new Mat4().setRotation(this);
    }
}
