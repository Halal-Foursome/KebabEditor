package org.halalfoursome.kebabeditor.playtest.physics;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * A collision volume in world space. Shapes follow their node's full transform,
 * rotation included, which an axis-aligned box could not.
 *
 * Every shape is convex, so the point nearest to any outside point is unique and
 * the actor's contacts come from closest-point queries alone.
 */
public sealed interface ConvexShape permits OrientedBox, Ellipsoid {

    /** The point of the shape nearest to {@code p}; {@code p} itself when it is inside. */
    Vec3 closestPoint(Vec3 p);

    /** For a point inside the shape: the nearest point on its surface and the outward normal there. */
    SurfacePoint nearestSurface(Vec3 p);

    /**
     * Distance along the ray to where it enters the shape, or none when it misses
     * within {@code maxDistance}. A ray starting inside hits at 0.
     *
     * @param direction unit length
     */
    Maybe<Float> raycast(Vec3 origin, Vec3 direction, float maxDistance);

    Aabb bounds();

    record SurfacePoint(Vec3 point, Vec3 normal) {}
}
