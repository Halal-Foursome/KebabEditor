package org.halalfoursome.kebabeditor.math;

import java.util.Objects;

public record Ray(Vec3 origin, Vec3 direction) {

    public Ray {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(direction, "direction");
        origin = origin.copy();
        direction = direction.normalize();
    }

    @Override
    public Vec3 origin() {
        return origin.copy();
    }

    @Override
    public Vec3 direction() {
        return direction.copy();
    }

    public Vec3 point(float t) {
        return origin.add(direction.mul(t));
    }
}
