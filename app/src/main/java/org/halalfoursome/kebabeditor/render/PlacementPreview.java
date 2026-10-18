package org.halalfoursome.kebabeditor.render;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;

/**
 * What the placement tool is about to spawn, for drawing its ghost.
 *
 * @param center    where the new object's center will be
 * @param rayStart  start of the visible pointer ray (just in front of and below the eye)
 * @param target    where the pointer ray ends: the surface point, or the center when floating
 * @param onSurface whether the ray rests on a surface rather than floating at wheel distance
 */
public record PlacementPreview(
    GreyboxShape shape,
    Vec3 size,
    Vec3 center,
    Vec3 rayStart,
    Vec3 target,
    boolean onSurface
) {

    public PlacementPreview {
        Objects.requireNonNull(shape, "shape");
        size = size.copy();
        center = center.copy();
        rayStart = rayStart.copy();
        target = target.copy();
    }

    @Override
    public Vec3 center() {
        return center.copy();
    }

    @Override
    public Vec3 size() {
        return size.copy();
    }
}
