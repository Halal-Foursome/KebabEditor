package org.halalfoursome.kebabeditor.playtest.reach;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

/**
 * Something the actor can stand on: the top of one solid, simplified to its
 * bounding box. Rotated boxes and spheres are treated as their bounds.
 *
 * @param node   the scene object the solid belongs to; for a prefab, the object referencing it
 * @param name   the solid's name, for the log
 * @param bounds the solid in world space
 */
public record Platform(SceneNode node, String name, Aabb bounds) {

    public Platform {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(name, "name");
        bounds = bounds.copy();
    }

    @Override
    public Aabb bounds() {
        return bounds.copy();
    }

    /** Height of the surface the feet stand on. */
    public float top() {
        return bounds.getMaxY();
    }

    /** Horizontal distance between the two footprints; 0 when they touch or overlap. */
    public float gap(Platform other) {
        float dx = Math.max(0, Math.max(bounds.getMinX() - other.bounds.getMaxX(), other.bounds.getMinX() - bounds.getMaxX()));
        float dz = Math.max(0, Math.max(bounds.getMinZ() - other.bounds.getMaxZ(), other.bounds.getMinZ() - bounds.getMaxZ()));
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** Whether {@code p} is over the footprint, give or take {@code margin}. */
    public boolean covers(Vec3 p, float margin) {
        return p.getX() >= bounds.getMinX() - margin && p.getX() <= bounds.getMaxX() + margin
            && p.getZ() >= bounds.getMinZ() - margin && p.getZ() <= bounds.getMaxZ() + margin;
    }

    /** The point on top of this platform nearest to {@code p}, horizontally. */
    public Vec3 topPointNear(Vec3 p) {
        return new Vec3(
            Math.clamp(p.getX(), bounds.getMinX(), bounds.getMaxX()),
            top(),
            Math.clamp(p.getZ(), bounds.getMinZ(), bounds.getMaxZ())
        );
    }

    public Vec3 topCenter() {
        return new Vec3(
            (bounds.getMinX() + bounds.getMaxX()) / 2,
            top(),
            (bounds.getMinZ() + bounds.getMaxZ()) / 2
        );
    }
}
