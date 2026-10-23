package org.halalfoursome.kebabeditor.playtest.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * The colliders of one playtest: solids block the actor, triggers only report it.
 * Queries scan the whole list after a bounds check, enough for hand-made levels.
 */
public final class PhysicsWorld {

    /** One volume and the name of the scene object it came from, for the log. */
    public record Collider(ConvexShape shape, String name, Aabb bounds, Maybe<PhysicsVolume> source) {

        public Collider {
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(name, "name");
            bounds = bounds.copy();
        }

        public Collider(ConvexShape shape, String name, Aabb bounds) {
            this(shape, name, bounds, Maybe.none());
        }

        public Collider(ConvexShape shape, String name) {
            this(shape, name, shape.bounds());
        }

        @Override
        public Aabb bounds() {
            return bounds.copy();
        }
    }

    private final List<Collider> solids;
    private final List<Collider> triggers;
    private final Maybe<Aabb> solidBounds;

    public PhysicsWorld(List<Collider> solids, List<Collider> triggers) {
        this.solids = List.copyOf(solids);
        this.triggers = List.copyOf(triggers);
        this.solidBounds = Maybe.fromOptional(this.solids.stream()
            .map(Collider::bounds)
            .reduce(Aabb::union));
    }

    /** Volumes that flatten to nothing are left out. */
    public static PhysicsWorld of(List<PhysicsVolume> volumes) {
        List<Collider> solids = new ArrayList<>();
        List<Collider> triggers = new ArrayList<>();

        for (PhysicsVolume volume : volumes) {
            volume.convexShape().ifSome(shape -> {
                Collider collider = new Collider(shape, volume.name(), shape.bounds(), Maybe.some(volume));
                switch (volume.role()) {
                    case SOLID -> solids.add(collider);
                    case TRIGGER -> triggers.add(collider);
                }
            });
        }

        return new PhysicsWorld(solids, triggers);
    }

    public List<Collider> solids() {
        return solids;
    }

    public List<Collider> triggers() {
        return triggers;
    }

    /** The box around every solid; none when there is nothing to stand on. */
    public Maybe<Aabb> solidBounds() {
        return solidBounds.map(Aabb::copy);
    }

    public List<Collider> solidsNear(Aabb area) {
        return near(solids, area);
    }

    public List<Collider> triggersTouching(Capsule capsule) {
        List<Collider> touching = new ArrayList<>();

        for (Collider trigger : near(triggers, capsule.bounds())) {
            if (capsule.overlaps(trigger.shape())) {
                touching.add(trigger);
            }
        }

        return touching;
    }

    /** Distance to the first solid along the ray, or none within {@code maxDistance}. */
    public Maybe<Float> raycast(Vec3 origin, Vec3 direction, float maxDistance) {
        Vec3 unit = direction.normalize();
        float nearest = Float.POSITIVE_INFINITY;

        for (Collider solid : solids) {
            float hit = solid.shape().raycast(origin, unit, maxDistance).unwrapOr(Float.POSITIVE_INFINITY);
            nearest = Math.min(nearest, hit);
        }

        return Float.isFinite(nearest) ? Maybe.some(nearest) : Maybe.none();
    }

    private static List<Collider> near(List<Collider> colliders, Aabb area) {
        List<Collider> result = new ArrayList<>();

        for (Collider collider : colliders) {
            if (collider.bounds.intersects(area)) {
                result.add(collider);
            }
        }

        return result;
    }
}
