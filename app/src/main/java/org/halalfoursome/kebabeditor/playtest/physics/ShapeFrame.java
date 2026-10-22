package org.halalfoursome.kebabeditor.playtest.physics;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Where a unit mesh (the [-0.5, 0.5] cube or the sphere of radius 0.5) ends up
 * under a world matrix: a center, three orthonormal axes and the half extent
 * along each.
 */
final class ShapeFrame {

    private static final float MIN_EXTENT = 1e-4f;

    final Vec3 center;
    final Vec3[] axes;
    final float[] half;

    private ShapeFrame(Vec3 center, Vec3[] axes, float[] half) {
        this.center = center;
        this.axes = axes;
        this.half = half;
    }

    /** None when the matrix flattens the shape along some axis. */
    static Maybe<ShapeFrame> of(Mat4 world) {
        Vec3 x = world.transformDirection(Vec3.unitX());
        Vec3 y = world.transformDirection(Vec3.unitY());
        Vec3 z = world.transformDirection(Vec3.unitZ());
        float[] half = { x.length() * 0.5f, y.length() * 0.5f, z.length() * 0.5f };

        for (float h : half) {
            if (!Float.isFinite(h) || h < MIN_EXTENT) {
                return Maybe.none();
            }
        }

        // Node scale is uniform, so the axes are already perpendicular; Gram-Schmidt
        // only removes float drift and keeps the frame right-handed.
        Vec3 ax = x.normalize();
        Vec3 ay = y.sub(ax.mul(y.dot(ax))).normalizeLocal();
        Vec3 az = ax.cross(ay);
        if (az.dot(z) < 0) {
            az.negateLocal();
        }

        return Maybe.some(new ShapeFrame(world.getTranslation(), new Vec3[] { ax, ay, az }, half));
    }

    float[] toLocal(Vec3 p) {
        Vec3 d = p.sub(center);
        return new float[] { d.dot(axes[0]), d.dot(axes[1]), d.dot(axes[2]) };
    }

    float[] directionToLocal(Vec3 d) {
        return new float[] { d.dot(axes[0]), d.dot(axes[1]), d.dot(axes[2]) };
    }

    Vec3 toWorld(float[] local) {
        return center.copy()
            .addLocal(axes[0].mul(local[0]))
            .addLocal(axes[1].mul(local[1]))
            .addLocal(axes[2].mul(local[2]));
    }

    Vec3 directionToWorld(float[] local) {
        return axes[0].mul(local[0])
            .addLocal(axes[1].mul(local[1]))
            .addLocal(axes[2].mul(local[2]));
    }

    /** World box around the box with half extents {@link #half}. */
    Aabb boxBounds() {
        float[] reach = new float[3];

        for (int world = 0; world < 3; world++) {
            for (int i = 0; i < 3; i++) {
                reach[world] += Math.abs(component(axes[i], world)) * half[i];
            }
        }

        return around(reach);
    }

    /** World box around the ellipsoid with semi-axes {@link #half}: tighter than its box's when rotated. */
    Aabb ellipsoidBounds() {
        float[] reach = new float[3];

        for (int world = 0; world < 3; world++) {
            float sum = 0f;
            for (int i = 0; i < 3; i++) {
                float r = component(axes[i], world) * half[i];
                sum += r * r;
            }
            reach[world] = (float) Math.sqrt(sum);
        }

        return around(reach);
    }

    private Aabb around(float[] reach) {
        return new Aabb(
            center.getX() - reach[0], center.getX() + reach[0],
            center.getY() - reach[1], center.getY() + reach[1],
            center.getZ() - reach[2], center.getZ() + reach[2]
        );
    }

    static float component(Vec3 v, int axis) {
        return switch (axis) {
            case 0 -> v.getX();
            case 1 -> v.getY();
            default -> v.getZ();
        };
    }
}
