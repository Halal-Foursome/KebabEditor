package org.halalfoursome.kebabeditor.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

class CameraTest {

    private static final float EPS = 1e-5f;

    @Test
    void levelCameraLooksDownNegativeZ() {
        Camera camera = levelCamera();

        assertTrue(new Vec3(0, 0, -1).approxEquals(camera.forward(), EPS));
        assertTrue(new Vec3(1, 0, 0).approxEquals(camera.right(), EPS));
    }

    @Test
    void negativeYawTurnsRight() {
        Camera camera = levelCamera();
        camera.rotate((float) -Math.PI / 2, 0);

        assertTrue(new Vec3(1, 0, 0).approxEquals(camera.forward(), EPS), "was " + camera.forward());
    }

    @Test
    void pitchIsClampedShortOfStraightUp() {
        Camera camera = levelCamera();
        camera.rotate(0, 10f);

        assertTrue(camera.getPitch() < Math.PI / 2);
        assertTrue(camera.forward().getY() > 0.99f);
    }

    @Test
    void viewMatrixMovesTheEyeToTheOrigin() {
        Camera camera = levelCamera();
        camera.move(new Vec3(3, 4, 5));

        Vec3 eyeInView = camera.view().transformPoint(camera.getPosition());
        assertTrue(Vec3.zero().approxEquals(eyeInView, 1e-4f), "was " + eyeInView);

        // A point straight ahead lands on the view-space -Z axis.
        Vec3 ahead = camera.view().transformPoint(camera.getPosition().add(camera.forward().mul(2)));
        assertEquals(-2f, ahead.getZ(), 1e-4f);
    }

    @Test
    void screenCenterRayIsTheViewDirectionAndCornersSpanTheFov() {
        Camera camera = levelCamera();
        camera.setFovY((float) Math.toRadians(90));

        assertTrue(camera.forward().approxEquals(camera.ray(0, 0, 2f).direction(), EPS));

        // With a 90 degree vertical FOV the top edge is 45 degrees up; aspect 2 doubles the sideways reach.
        Vec3 topRight = camera.ray(1, 1, 2f).direction();
        assertEquals(1f, topRight.getY() / -topRight.getZ(), 1e-4f);
        assertEquals(2f, topRight.getX() / -topRight.getZ(), 1e-4f);
    }

    private static Camera levelCamera() {
        Camera camera = new Camera();
        camera.rotate(-camera.getYaw(), -camera.getPitch());
        camera.move(camera.getPosition().negate());
        return camera;
    }
}
