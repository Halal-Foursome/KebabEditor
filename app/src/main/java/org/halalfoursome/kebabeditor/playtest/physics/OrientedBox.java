package org.halalfoursome.kebabeditor.playtest.physics;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** A box with its own rotation: the unit cube under a node's world matrix. */
public final class OrientedBox implements ConvexShape {

    private final ShapeFrame frame;
    private final Aabb bounds;

    private OrientedBox(ShapeFrame frame) {
        this.frame = frame;
        this.bounds = frame.boxBounds();
    }

    /** The [-0.5, 0.5] cube under {@code world}; none when the matrix flattens it. */
    public static Maybe<OrientedBox> of(Mat4 world) {
        return ShapeFrame.of(world).map(OrientedBox::new);
    }

    public Vec3 center() {
        return frame.center.copy();
    }

    /** Half the box's size along each of its own axes. */
    public Vec3 halfExtents() {
        return new Vec3(frame.half[0], frame.half[1], frame.half[2]);
    }

    @Override
    public Vec3 closestPoint(Vec3 p) {
        float[] local = frame.toLocal(p);

        for (int i = 0; i < 3; i++) {
            local[i] = Math.clamp(local[i], -frame.half[i], frame.half[i]);
        }

        return frame.toWorld(local);
    }

    // The face with the least depth is the shortest way out.
    @Override
    public SurfacePoint nearestSurface(Vec3 p) {
        float[] local = frame.toLocal(p);
        int axis = 0;
        float least = Float.POSITIVE_INFINITY;

        for (int i = 0; i < 3; i++) {
            float depth = frame.half[i] - Math.abs(local[i]);
            if (depth < least) {
                least = depth;
                axis = i;
            }
        }

        float sign = local[axis] < 0 ? -1f : 1f;
        local[axis] = sign * frame.half[axis];

        return new SurfacePoint(frame.toWorld(local), frame.axes[axis].mul(sign));
    }

    // Slab test in the box's own space.
    @Override
    public Maybe<Float> raycast(Vec3 origin, Vec3 direction, float maxDistance) {
        float[] o = frame.toLocal(origin);
        float[] d = frame.directionToLocal(direction);
        float enter = 0f;
        float exit = maxDistance;

        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-8f) {
                if (Math.abs(o[i]) > frame.half[i]) {
                    return Maybe.none();
                }
                continue;
            }

            float t1 = (-frame.half[i] - o[i]) / d[i];
            float t2 = (frame.half[i] - o[i]) / d[i];
            enter = Math.max(enter, Math.min(t1, t2));
            exit = Math.min(exit, Math.max(t1, t2));

            if (enter > exit) {
                return Maybe.none();
            }
        }

        return Maybe.some(enter);
    }

    @Override
    public Aabb bounds() {
        return bounds.copy();
    }
}
