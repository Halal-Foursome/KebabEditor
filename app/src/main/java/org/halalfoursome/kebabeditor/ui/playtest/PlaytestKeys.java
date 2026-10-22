package org.halalfoursome.kebabeditor.ui.playtest;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import org.halalfoursome.kebabeditor.math.Vec3;

/**
 * The playtest's keyboard state: WASD or the arrow keys run, Space jumps.
 * Turns held keys into a move direction relative to where the camera looks.
 */
public final class PlaytestKeys {

    private final Set<Integer> held = new HashSet<>();
    private boolean jumpPressed;

    public void press(int keyCode) {
        // Held keys repeat; only the first press of Space is a jump.
        if (held.add(keyCode) && keyCode == KeyEvent.VK_SPACE) {
            jumpPressed = true;
        }
    }

    public void release(int keyCode) {
        held.remove(keyCode);
    }

    /** Forget held keys, e.g. when focus is lost and their releases will not arrive. */
    public void clear() {
        held.clear();
    }

    /** Whether Space went down since the last call. */
    public boolean takeJump() {
        boolean pressed = jumpPressed;
        jumpPressed = false;
        return pressed;
    }

    /**
     * The wished horizontal direction, at most unit length.
     *
     * @param yaw the camera's facing: forward is where it looks, flattened onto the ground
     */
    public Vec3 move(float yaw) {
        float forward = axis(KeyEvent.VK_W, KeyEvent.VK_UP) - axis(KeyEvent.VK_S, KeyEvent.VK_DOWN);
        float right = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - axis(KeyEvent.VK_A, KeyEvent.VK_LEFT);

        Vec3 move = new Vec3((float) -Math.sin(yaw), 0, (float) -Math.cos(yaw)).mulLocal(forward)
            .addLocal(new Vec3((float) Math.cos(yaw), 0, (float) -Math.sin(yaw)).mulLocal(right));

        return move.lengthSquared() > 1 ? move.normalizeLocal() : move;
    }

    private float axis(int key, int alternative) {
        return held.contains(key) || held.contains(alternative) ? 1f : 0f;
    }
}
