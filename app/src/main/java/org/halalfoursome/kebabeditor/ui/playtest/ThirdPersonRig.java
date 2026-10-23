package org.halalfoursome.kebabeditor.ui.playtest;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.render.Camera;

/**
 * A camera orbiting a target at a distance. It stops short of solids between it
 * and the target, so walls do not hide the actor.
 */
public final class ThirdPersonRig {

    static final float START_PITCH = (float) Math.toRadians(-20);
    static final float MIN_PITCH = (float) Math.toRadians(-80);
    static final float MAX_PITCH = (float) Math.toRadians(30);
    static final float START_DISTANCE = 6f;
    static final float MIN_DISTANCE = 1.5f;
    static final float MAX_DISTANCE = 20f;
    /** Gap kept between the camera and a solid it would otherwise sit in. */
    static final float CLEARANCE = 0.25f;

    private static final double LOOK_DEGREES_PER_PIXEL = 0.25;
    private static final float WHEEL_STEP = 0.8f;
    private static final float CLOSEST = 0.1f;

    private float yaw;
    private float pitch = START_PITCH;
    private float distance = START_DISTANCE;

    /** @param yaw start looking the way the actor faces */
    public ThirdPersonRig(float yaw) {
        this.yaw = yaw;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float distance() {
        return distance;
    }

    /** Turn by a mouse drag: dragging right turns right, dragging up looks up. */
    public void orbit(int dxPixels, int dyPixels) {
        yaw -= (float) Math.toRadians(dxPixels * LOOK_DEGREES_PER_PIXEL);
        pitch = Math.clamp(pitch - (float) Math.toRadians(dyPixels * LOOK_DEGREES_PER_PIXEL), MIN_PITCH, MAX_PITCH);
    }

    /** Wheel towards the user moves the camera back. */
    public void zoom(double wheelRotation) {
        distance = Math.clamp(distance + (float) wheelRotation * WHEEL_STEP, MIN_DISTANCE, MAX_DISTANCE);
    }

    /** Point the camera at {@code target} from behind, in front of any solid in the way. */
    public void place(Camera camera, Vec3 target, PhysicsWorld world) {
        camera.setOrientation(yaw, pitch);

        Vec3 back = camera.forward().negateLocal();
        float reach = world.raycast(target, back, distance + CLEARANCE)
            .map(hit -> Math.max(hit - CLEARANCE, CLOSEST))
            .unwrapOr(distance);

        camera.getPosition().set(target.add(back.mulLocal(Math.min(reach, distance))));
    }
}
