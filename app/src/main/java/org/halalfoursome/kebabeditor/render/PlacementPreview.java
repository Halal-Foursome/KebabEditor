package org.halalfoursome.kebabeditor.render;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.GreyboxShape;

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
