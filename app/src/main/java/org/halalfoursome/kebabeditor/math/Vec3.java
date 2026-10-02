package org.halalfoursome.kebabeditor.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vec3 {

    private float x;
    private float y;
    private float z;

    public static Vec3 zero() {
        return new Vec3(0, 0, 0);
    }

    public static Vec3 one() {
        return new Vec3(1, 1, 1);
    }

    public static Vec3 unitX() {
        return new Vec3(1, 0, 0);
    }

    public static Vec3 unitY() {
        return new Vec3(0, 1, 0);
    }

    public static Vec3 unitZ() {
        return new Vec3(0, 0, 1);
    }

    public Vec3 copy() {
        return new Vec3(x, y, z);
    }

    public Vec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vec3 set(Vec3 o) {
        return set(o.x, o.y, o.z);
    }

    public Vec3 addLocal(float ox, float oy, float oz) {
        return set(x + ox, y + oy, z + oz);
    }

    public Vec3 addLocal(Vec3 o) {
        return addLocal(o.x, o.y, o.z);
    }

    public Vec3 subLocal(Vec3 o) {
        return set(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 mulLocal(float s) {
        return set(x * s, y * s, z * s);
    }

    public Vec3 mulLocal(Vec3 o) {
        return set(x * o.x, y * o.y, z * o.z);
    }

    public Vec3 divLocal(float s) {
        return mulLocal(1f / s);
    }

    public Vec3 negateLocal() {
        return set(-x, -y, -z);
    }

    public Vec3 crossLocal(Vec3 o) {
        return set(
            y * o.z - z * o.y,
            z * o.x - x * o.z,
            x * o.y - y * o.x
        );
    }

    public Vec3 normalizeLocal() {
        float len = length();
        return len == 0 ? set(0, 0, 0) : divLocal(len);
    }

    public Vec3 lerpLocal(Vec3 o, float t) {
        return set(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }

    public Vec3 minLocal(Vec3 o) {
        return set(Math.min(x, o.x), Math.min(y, o.y), Math.min(z, o.z));
    }

    public Vec3 maxLocal(Vec3 o) {
        return set(Math.max(x, o.x), Math.max(y, o.y), Math.max(z, o.z));
    }

    public Vec3 absLocal() {
        return set(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public Vec3 reflectLocal(Vec3 normal) {
        float d = 2f * dot(normal);
        return set(x - normal.x * d, y - normal.y * d, z - normal.z * d);
    }

    public Vec3 projectLocal(Vec3 onto) {
        float k = dot(onto) / onto.lengthSquared();
        return set(onto.x * k, onto.y * k, onto.z * k);
    }

    public Vec3 add(Vec3 o) {
        return copy().addLocal(o);
    }

    public Vec3 sub(Vec3 o) {
        return copy().subLocal(o);
    }

    public Vec3 mul(float s) {
        return copy().mulLocal(s);
    }

    public Vec3 mul(Vec3 o) {
        return copy().mulLocal(o);
    }

    public Vec3 div(float s) {
        return copy().divLocal(s);
    }

    public Vec3 negate() {
        return copy().negateLocal();
    }

    public Vec3 cross(Vec3 o) {
        return copy().crossLocal(o);
    }

    public Vec3 normalize() {
        return copy().normalizeLocal();
    }

    public Vec3 lerp(Vec3 o, float t) {
        return copy().lerpLocal(o, t);
    }

    public Vec3 min(Vec3 o) {
        return copy().minLocal(o);
    }

    public Vec3 max(Vec3 o) {
        return copy().maxLocal(o);
    }

    public Vec3 abs() {
        return copy().absLocal();
    }

    public Vec3 reflect(Vec3 normal) {
        return copy().reflectLocal(normal);
    }

    public Vec3 project(Vec3 onto) {
        return copy().projectLocal(onto);
    }

    public float dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public float lengthSquared() {
        return dot(this);
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float distanceSquared(Vec3 o) {
        float dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float distance(Vec3 o) {
        return (float) Math.sqrt(distanceSquared(o));
    }

    public float angle(Vec3 o) {
        float d = dot(o) / (length() * o.length());
        return (float) Math.acos(Math.max(-1f, Math.min(1f, d)));
    }

    public boolean approxEquals(Vec3 o, float eps) {
        return Math.abs(x - o.x) <= eps && Math.abs(y - o.y) <= eps && Math.abs(z - o.z) <= eps;
    }

    public float[] toArray() {
        return new float[] { x, y, z };
    }
}
