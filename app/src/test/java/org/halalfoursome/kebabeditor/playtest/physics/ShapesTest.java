package org.halalfoursome.kebabeditor.playtest.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

class ShapesTest {

    private static final float EPS = 1e-4f;

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertTrue(expected.approxEquals(actual, EPS), "expected " + expected + " but was " + actual);
    }

    private static Mat4 placed(Vec3 at, Quat rotation, Vec3 size) {
        return Mat4.compose(at, rotation, size);
    }

    @Test
    void boxClampsOutsidePointsToItsSurface() {
        OrientedBox box = OrientedBox.of(placed(new Vec3(0, 0, 0), Quat.identity(), new Vec3(4, 2, 2))).unwrap();

        assertVec(new Vec3(2, 1, 0), box.closestPoint(new Vec3(5, 3, 0)));
        assertVec(new Vec3(0.5f, 0.2f, -0.3f), box.closestPoint(new Vec3(0.5f, 0.2f, -0.3f)));
    }

    @Test
    void rotatedBoxFollowsItsOwnAxes() {
        // A 4 x 1 x 1 bar turned 90° about Y now runs along Z.
        Quat turn = Quat.fromAxisAngle(Vec3.unitY(), (float) Math.toRadians(90));
        OrientedBox bar = OrientedBox.of(placed(Vec3.zero(), turn, new Vec3(4, 1, 1))).unwrap();

        assertVec(new Vec3(0, 0, 2), bar.closestPoint(new Vec3(0, 0, 10)));
        assertVec(new Vec3(0.5f, 0, 0), bar.closestPoint(new Vec3(10, 0, 0)));

        Aabb bounds = bar.bounds();
        assertEquals(0.5f, bounds.getMaxX(), EPS);
        assertEquals(2f, bounds.getMaxZ(), EPS);
    }

    @Test
    void boxEscapesThroughTheNearestFace() {
        OrientedBox box = OrientedBox.of(placed(Vec3.zero(), Quat.identity(), new Vec3(4, 2, 4))).unwrap();

        ConvexShape.SurfacePoint surface = box.nearestSurface(new Vec3(0.5f, 0.8f, 0));

        assertVec(new Vec3(0, 1, 0), surface.normal());
        assertVec(new Vec3(0.5f, 1, 0), surface.point());
    }

    @Test
    void boxRaycastHitsTheEntryFace() {
        OrientedBox box = OrientedBox.of(placed(new Vec3(5, 0, 0), Quat.identity(), Vec3.one())).unwrap();

        assertEquals(4.5f, box.raycast(Vec3.zero(), Vec3.unitX(), 10).unwrap(), EPS);
        assertTrue(box.raycast(Vec3.zero(), Vec3.unitY(), 10).isNone());
        assertTrue(box.raycast(Vec3.zero(), Vec3.unitX(), 3).isNone());
    }

    @Test
    void flatMatrixMakesNoShape() {
        assertTrue(OrientedBox.of(Mat4.scaling(new Vec3(1, 0, 1))).isNone());
    }

    @Test
    void sphereClosestPointIsAlongTheRadius() {
        Ellipsoid sphere = Ellipsoid.of(placed(new Vec3(1, 0, 0), Quat.identity(), new Vec3(2, 2, 2))).unwrap();

        assertVec(new Vec3(1, 1, 0), sphere.closestPoint(new Vec3(1, 5, 0)));
        // Centered at x = 1 with radius 1, so the ray from x = -5 enters at x = 0.
        assertEquals(5f, sphere.raycast(new Vec3(-5, 0, 0), Vec3.unitX(), 10).unwrap(), EPS);
    }

    @Test
    void ellipsoidClosestPointLiesOnTheSurfaceAndIsNearest() {
        // Semi-axes 3, 1, 2.
        Ellipsoid ellipsoid = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(6, 2, 4))).unwrap();
        Vec3 outside = new Vec3(4, 3, 1);

        Vec3 closest = ellipsoid.closestPoint(outside);
        float onSurface = sq(closest.getX() / 3) + sq(closest.getY() / 1) + sq(closest.getZ() / 2);
        assertEquals(1f, onSurface, 1e-3f);

        // No sampled surface point is nearer.
        float best = closest.distance(outside);
        for (int i = 0; i < 64; i++) {
            for (int j = 0; j <= 32; j++) {
                double phi = 2 * Math.PI * i / 64;
                double theta = Math.PI * j / 32;
                Vec3 sample = new Vec3(
                    (float) (3 * Math.sin(theta) * Math.cos(phi)),
                    (float) (1 * Math.cos(theta)),
                    (float) (2 * Math.sin(theta) * Math.sin(phi))
                );
                assertTrue(sample.distance(outside) >= best - 1e-3f);
            }
        }
    }

    @Test
    void ellipsoidRaycastUsesItsStretchedShape() {
        Ellipsoid ellipsoid = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(6, 2, 2))).unwrap();

        assertEquals(7f, ellipsoid.raycast(new Vec3(-10, 0, 0), Vec3.unitX(), 20).unwrap(), EPS);
        assertEquals(9f, ellipsoid.raycast(new Vec3(0, -10, 0), Vec3.unitY(), 20).unwrap(), EPS);
    }

    @Test
    void pointInsideASphereEscapesRadially() {
        Ellipsoid sphere = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(4, 4, 4))).unwrap();

        ConvexShape.SurfacePoint surface = sphere.nearestSurface(new Vec3(0.5f, 0, 0));

        assertVec(new Vec3(2, 0, 0), surface.point());
        assertVec(Vec3.unitX(), surface.normal());
        // Inside, the closest point is the point itself.
        assertVec(new Vec3(0.5f, 0, 0), sphere.closestPoint(new Vec3(0.5f, 0, 0)));
    }

    @Test
    void ellipsoidCenterEscapesAlongItsThinnestAxis() {
        // Semi-axes 3, 0.5, 2: the thinnest is Y.
        Ellipsoid flat = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(6, 1, 4))).unwrap();

        ConvexShape.SurfacePoint surface = flat.nearestSurface(Vec3.zero());

        assertVec(new Vec3(0, 0.5f, 0), surface.point());
        assertVec(Vec3.unitY(), surface.normal());
    }

    @Test
    void pointInsideAnEllipsoidEscapesOutwards() {
        Ellipsoid ellipsoid = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(6, 2, 4))).unwrap();
        Vec3 inside = new Vec3(1, 0.3f, 0.5f);

        ConvexShape.SurfacePoint surface = ellipsoid.nearestSurface(inside);

        Vec3 p = surface.point();
        assertEquals(1f, sq(p.getX() / 3) + sq(p.getY() / 1) + sq(p.getZ() / 2), 1e-3f);
        assertTrue(surface.normal().dot(p.sub(inside)) > 0, "the normal points out of the shape");
        assertEquals(1f, surface.normal().length(), EPS);
    }

    @Test
    void ellipsoidRaycastFromInsideHitsAtOnceAndMissesWhenPointingAway() {
        Ellipsoid ball = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(2, 2, 2))).unwrap();

        assertEquals(0f, ball.raycast(Vec3.zero(), Vec3.unitX(), 10).unwrap(), EPS);
        assertTrue(ball.raycast(new Vec3(5, 0, 0), Vec3.unitX(), 10).isNone());
        assertTrue(ball.raycast(new Vec3(-5, 3, 0), Vec3.unitX(), 10).isNone());
        assertTrue(ball.raycast(new Vec3(-5, 0, 0), Vec3.unitX(), 2).isNone());
    }

    @Test
    void ellipsoidReportsItsPlacement() {
        Ellipsoid ellipsoid = Ellipsoid.of(placed(new Vec3(1, 2, 3), Quat.identity(), new Vec3(6, 2, 4))).unwrap();

        assertVec(new Vec3(1, 2, 3), ellipsoid.center());
        assertVec(new Vec3(3, 1, 2), ellipsoid.semiAxes());
        assertTrue(Ellipsoid.of(Mat4.scaling(new Vec3(1, 1, 0))).isNone());
    }

    @Test
    void boxReportsItsPlacementAndRayAlongAFaceMisses() {
        OrientedBox box = OrientedBox.of(placed(new Vec3(1, 2, 3), Quat.identity(), new Vec3(2, 4, 6))).unwrap();

        assertVec(new Vec3(1, 2, 3), box.center());
        assertVec(new Vec3(1, 2, 3), box.halfExtents());
        // Parallel to X, but above the box.
        assertTrue(box.raycast(new Vec3(-10, 10, 3), Vec3.unitX(), 100).isNone());
    }

    @Test
    void capsuleStandingOnABoxGetsAnUpwardContact() {
        OrientedBox floor = OrientedBox.of(placed(new Vec3(0, -0.5f, 0), Quat.identity(), new Vec3(10, 1, 10))).unwrap();
        Capsule capsule = Capsule.standing(new Vec3(0, -0.1f, 0), 0.4f, 1.8f);

        Capsule.Contact contact = capsule.contact(floor).unwrap();

        assertVec(Vec3.unitY(), contact.normal());
        assertEquals(0.1f, contact.depth(), EPS);
    }

    @Test
    void capsuleAgainstARotatedWallIsPushedAlongTheWallNormal() {
        Quat turn = Quat.fromAxisAngle(Vec3.unitY(), (float) Math.toRadians(45));
        OrientedBox wall = OrientedBox.of(placed(Vec3.zero(), turn, new Vec3(1, 4, 10))).unwrap();
        // The wall's +X face normal after turning 45° about Y.
        Vec3 normal = turn.rotate(Vec3.unitX());
        Vec3 feet = normal.mul(0.5f + 0.3f).addLocal(0, -1, 0);

        Capsule.Contact contact = Capsule.standing(feet, 0.4f, 1.8f).contact(wall).unwrap();

        assertVec(normal, contact.normal());
        assertEquals(0.1f, contact.depth(), 1e-3f);
    }

    @Test
    void capsuleClearOfAShapeHasNoContact() {
        Ellipsoid ball = Ellipsoid.of(placed(Vec3.zero(), Quat.identity(), new Vec3(2, 2, 2))).unwrap();

        assertTrue(Capsule.standing(new Vec3(2, 0, 0), 0.4f, 1.8f).contact(ball).isNone());
        assertTrue(Capsule.standing(new Vec3(1.3f, 0, 0), 0.4f, 1.8f).contact(ball).isSome());
    }

    @Test
    void capsuleCoreInsideAShapeStillFindsAWayOut() {
        OrientedBox block = OrientedBox.of(placed(Vec3.zero(), Quat.identity(), new Vec3(4, 4, 4))).unwrap();
        Capsule capsule = Capsule.standing(new Vec3(1.8f, -0.5f, 0), 0.3f, 1f);

        Capsule.Contact contact = capsule.contact(block).unwrap();

        assertVec(Vec3.unitX(), contact.normal());
        assertEquals(0.3f + 0.2f, contact.depth(), EPS);
    }

    private static float sq(float value) {
        return value * value;
    }
}
