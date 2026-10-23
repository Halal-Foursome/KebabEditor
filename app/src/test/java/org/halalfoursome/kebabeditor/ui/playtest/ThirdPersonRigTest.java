package org.halalfoursome.kebabeditor.ui.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsVolume;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.render.Camera;
import org.junit.jupiter.api.Test;

class ThirdPersonRigTest {

    private static final float EPS = 1e-4f;
    private static final PhysicsWorld EMPTY = PhysicsWorld.of(List.of());

    @Test
    void startsBehindAndAboveLookingTheWayTheActorFaces() {
        ThirdPersonRig rig = new ThirdPersonRig(0.5f);

        assertEquals(0.5f, rig.yaw(), EPS);
        assertEquals(ThirdPersonRig.START_PITCH, rig.pitch(), EPS);
        assertEquals(ThirdPersonRig.START_DISTANCE, rig.distance(), EPS);
    }

    @Test
    void orbitTurnsWithTheDragAndKeepsPitchInRange() {
        ThirdPersonRig rig = new ThirdPersonRig(0f);

        rig.orbit(100, 0);
        assertTrue(rig.yaw() < 0, "dragging right turns right");

        rig.orbit(0, -100_000);
        assertEquals(ThirdPersonRig.MAX_PITCH, rig.pitch(), EPS);
        rig.orbit(0, 100_000);
        assertEquals(ThirdPersonRig.MIN_PITCH, rig.pitch(), EPS);
    }

    @Test
    void zoomStaysInRange() {
        ThirdPersonRig rig = new ThirdPersonRig(0f);

        rig.zoom(1000);
        assertEquals(ThirdPersonRig.MAX_DISTANCE, rig.distance(), EPS);
        rig.zoom(-1000);
        assertEquals(ThirdPersonRig.MIN_DISTANCE, rig.distance(), EPS);
    }

    @Test
    void cameraLooksAtTheTargetFromItsDistance() {
        ThirdPersonRig rig = new ThirdPersonRig(0f);
        Camera camera = new Camera();
        Vec3 target = new Vec3(1, 2, 3);

        rig.place(camera, target, EMPTY);

        assertEquals(ThirdPersonRig.START_DISTANCE, camera.getPosition().distance(target), EPS);
        Vec3 toTarget = target.sub(camera.getPosition()).normalizeLocal();
        assertTrue(toTarget.approxEquals(camera.forward(), EPS));
    }

    @Test
    void wallBehindTheTargetPullsTheCameraIn() {
        // Looking along -Z and slightly down, so the camera sits behind the target (+Z) and a little above.
        ThirdPersonRig rig = new ThirdPersonRig(0f);
        Camera camera = new Camera();
        // A wall 2 m behind the target along +Z.
        PhysicsWorld world = PhysicsWorld.of(List.of(new PhysicsVolume(
            "wall", PhysicsVolume.Shape.BOX,
            Mat4.compose(new Vec3(0, 0, 2.5f), Quat.identity(), new Vec3(40, 40, 1)),
            PhysicsVolume.Role.SOLID
        )));

        rig.place(camera, Vec3.zero(), world);

        assertTrue(camera.getPosition().getZ() < 2f, "camera at " + camera.getPosition());
        assertTrue(camera.getPosition().length() < ThirdPersonRig.START_DISTANCE);
    }
}
