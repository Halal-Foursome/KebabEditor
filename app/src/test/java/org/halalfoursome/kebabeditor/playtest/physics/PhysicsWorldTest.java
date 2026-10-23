package org.halalfoursome.kebabeditor.playtest.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.junit.jupiter.api.Test;

class PhysicsWorldTest {

    private static final float EPS = 1e-4f;

    private static PhysicsVolume box(String name, Vec3 center, Vec3 size, PhysicsVolume.Role role) {
        return new PhysicsVolume(name, PhysicsVolume.Shape.BOX, Mat4.compose(center, Quat.identity(), size), role);
    }

    private static List<String> names(List<Collider> colliders) {
        return colliders.stream().map(Collider::name).toList();
    }

    @Test
    void volumesAreSortedIntoSolidsAndTriggers() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("floor", Vec3.zero(), new Vec3(10, 1, 10), PhysicsVolume.Role.SOLID),
            box("goal", new Vec3(0, 2, 0), Vec3.one(), PhysicsVolume.Role.TRIGGER),
            new PhysicsVolume("ball", PhysicsVolume.Shape.SPHERE, Mat4.translation(new Vec3(3, 1, 0)), PhysicsVolume.Role.SOLID)
        ));

        assertEquals(List.of("floor", "ball"), names(world.solids()));
        assertEquals(List.of("goal"), names(world.triggers()));
        assertTrue(world.solids().get(1).shape() instanceof Ellipsoid);
    }

    @Test
    void flatVolumesAreLeftOut() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("sheet", Vec3.zero(), new Vec3(1, 0, 1), PhysicsVolume.Role.SOLID)
        ));

        assertTrue(world.solids().isEmpty());
        assertTrue(world.solidBounds().isNone());
    }

    @Test
    void solidBoundsCoverEverySolidButNotTriggers() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("a", new Vec3(-2, 0, 0), Vec3.one(), PhysicsVolume.Role.SOLID),
            box("b", new Vec3(2, 1, 0), Vec3.one(), PhysicsVolume.Role.SOLID),
            box("far", new Vec3(50, 50, 50), Vec3.one(), PhysicsVolume.Role.TRIGGER)
        ));

        Aabb bounds = world.solidBounds().unwrap();

        assertEquals(-2.5f, bounds.getMinX(), EPS);
        assertEquals(2.5f, bounds.getMaxX(), EPS);
        assertEquals(1.5f, bounds.getMaxY(), EPS);
    }

    @Test
    void solidsNearOnlyReturnsThoseWhoseBoundsTouchTheArea() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("near", Vec3.zero(), Vec3.one(), PhysicsVolume.Role.SOLID),
            box("far", new Vec3(20, 0, 0), Vec3.one(), PhysicsVolume.Role.SOLID)
        ));

        assertEquals(List.of("near"), names(world.solidsNear(new Aabb(-1, 1, -1, 1, -1, 1))));
    }

    @Test
    void triggersTouchingAreThoseTheCapsuleOverlaps() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("here", new Vec3(0, 1, 0), Vec3.one(), PhysicsVolume.Role.TRIGGER),
            box("there", new Vec3(5, 1, 0), Vec3.one(), PhysicsVolume.Role.TRIGGER)
        ));

        Capsule capsule = Capsule.standing(Vec3.zero(), 0.3f, 1.8f);

        assertEquals(List.of("here"), names(world.triggersTouching(capsule)));
    }

    @Test
    void raycastFindsTheNearestSolidAndIgnoresTriggers() {
        PhysicsWorld world = PhysicsWorld.of(List.of(
            box("far wall", new Vec3(10, 0, 0), Vec3.one(), PhysicsVolume.Role.SOLID),
            box("near wall", new Vec3(5, 0, 0), Vec3.one(), PhysicsVolume.Role.SOLID),
            box("mist", new Vec3(2, 0, 0), Vec3.one(), PhysicsVolume.Role.TRIGGER)
        ));

        assertEquals(4.5f, world.raycast(Vec3.zero(), new Vec3(2, 0, 0), 100).unwrap(), EPS);
        assertTrue(world.raycast(Vec3.zero(), Vec3.unitY(), 100).isNone());
        assertTrue(world.raycast(Vec3.zero(), Vec3.unitX(), 4).isNone());
    }

    @Test
    void volumeInsideAPrefabIsPlacedAndNamedByItsOwner() {
        PhysicsVolume inner = box("wall", new Vec3(1, 0, 0), Vec3.one(), PhysicsVolume.Role.SOLID);

        PhysicsVolume placed = inner.placedIn(Mat4.translation(new Vec3(10, 0, 0)), "tower");

        assertEquals("tower > wall", placed.name());
        assertTrue(new Vec3(11, 0, 0).approxEquals(placed.world().getTranslation(), EPS));
        assertEquals(inner.shape(), placed.shape());
        assertEquals(inner.role(), placed.role());
    }

    @Test
    void rotatedSphereBoundsAreTight() {
        Mat4 world = Mat4.compose(Vec3.zero(), Quat.fromAxisAngle(new Vec3(1, 1, 0), 0.7f), new Vec3(2, 2, 2));

        Aabb bounds = Ellipsoid.of(world).unwrap().bounds();

        assertEquals(1f, bounds.getMaxX(), EPS);
        assertEquals(-1f, bounds.getMinY(), EPS);
    }
}
