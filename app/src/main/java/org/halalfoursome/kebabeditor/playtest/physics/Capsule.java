package org.halalfoursome.kebabeditor.playtest.physics;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * An upright capsule: every point within {@code radius} of the segment from
 * {@code bottom} to {@code top}. The rounded bottom slides over small edges and
 * lets the actor stand on slopes without catching on corners.
 */
public record Capsule(Vec3 bottom, Vec3 top, float radius) {

    private static final int MAX_ITERATIONS = 16;
    private static final float SETTLED = 1e-10f;
    private static final float TOUCHING = 1e-5f;

    /** How to push the capsule out of a shape: along {@code normal} by {@code depth}. */
    public record Contact(Vec3 normal, float depth) {}

    public Capsule {
        bottom = bottom.copy();
        top = top.copy();
    }

    /** A capsule standing on {@code feet}, {@code height} tall overall. */
    public static Capsule standing(Vec3 feet, float radius, float height) {
        return new Capsule(
            feet.add(new Vec3(0, radius, 0)),
            feet.add(new Vec3(0, height - radius, 0)),
            radius
        );
    }

    @Override
    public Vec3 bottom() {
        return bottom.copy();
    }

    @Override
    public Vec3 top() {
        return top.copy();
    }

    public Aabb bounds() {
        return new Aabb(
            Math.min(bottom.getX(), top.getX()) - radius, Math.max(bottom.getX(), top.getX()) + radius,
            Math.min(bottom.getY(), top.getY()) - radius, Math.max(bottom.getY(), top.getY()) + radius,
            Math.min(bottom.getZ(), top.getZ()) - radius, Math.max(bottom.getZ(), top.getZ()) + radius
        );
    }

    /**
     * Penetration into a shape, or none when they do not overlap.
     *
     * The closest pair between the core segment and the convex shape is found by
     * projecting back and forth between the two, which converges because both are
     * convex. When the segment itself has entered the shape, the shape's nearest
     * surface decides the way out.
     */
    public Maybe<Contact> contact(ConvexShape shape) {
        Vec3 q = shape.closestPoint(bottom.lerp(top, 0.5f));
        Vec3 p = closestOnSegment(q);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Vec3 nextQ = shape.closestPoint(p);
            Vec3 nextP = closestOnSegment(nextQ);
            boolean settled = nextQ.distanceSquared(q) < SETTLED && nextP.distanceSquared(p) < SETTLED;
            q = nextQ;
            p = nextP;
            if (settled) {
                break;
            }
        }

        Vec3 gap = p.sub(q);
        float distance = gap.length();

        if (distance >= radius) {
            return Maybe.none();
        }
        if (distance > TOUCHING) {
            return Maybe.some(new Contact(gap.divLocal(distance), radius - distance));
        }

        ConvexShape.SurfacePoint surface = shape.nearestSurface(p);
        return Maybe.some(new Contact(surface.normal(), radius + surface.point().distance(p)));
    }

    public boolean overlaps(ConvexShape shape) {
        return contact(shape).isSome();
    }

    Vec3 closestOnSegment(Vec3 point) {
        Vec3 axis = top.sub(bottom);
        float lengthSquared = axis.lengthSquared();
        if (lengthSquared < 1e-12f) {
            return bottom.copy();
        }

        float t = Math.clamp(point.sub(bottom).dot(axis) / lengthSquared, 0f, 1f);
        return bottom.add(axis.mulLocal(t));
    }
}
