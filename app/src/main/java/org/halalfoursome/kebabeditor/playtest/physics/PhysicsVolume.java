package org.halalfoursome.kebabeditor.playtest.physics;

import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * What one scene object contributes to playtest physics: a unit shape placed by
 * a world matrix. Archetype behaviors produce these from the same matrix they
 * draw with, so what collides is what is shown.
 *
 * @param name  the scene object it came from, for the log
 * @param shape the [-0.5, 0.5] cube or the sphere of radius 0.5
 * @param world places the unit shape in the level
 */
public record PhysicsVolume(String name, Shape shape, Mat4 world, Role role) {

    public enum Shape {
        BOX,
        SPHERE,
    }

    public enum Role {
        /** Blocks the actor. */
        SOLID,
        /** Reports the actor entering it. */
        TRIGGER,
    }

    public PhysicsVolume {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(role, "role");
        world = world.copy();
    }

    @Override
    public Mat4 world() {
        return world.copy();
    }

    /** The same volume inside a prefab placed by {@code base}, named after the referencing object. */
    public PhysicsVolume placedIn(Mat4 base, String owner) {
        return new PhysicsVolume(owner + " > " + name, shape, base.mul(world), role);
    }

    /** None when the matrix flattens the shape. */
    public Maybe<ConvexShape> convexShape() {
        return switch (shape) {
            case BOX -> OrientedBox.of(world).map(ConvexShape.class::cast);
            case SPHERE -> Ellipsoid.of(world).map(ConvexShape.class::cast);
        };
    }
}
