package org.halalfoursome.kebabeditor.playtest;

import java.util.Locale;

import org.halalfoursome.kebabeditor.math.Vec3;

/**
 * One jump from take-off to landing, measured at the actor's feet.
 *
 * @param apexY highest the feet got while airborne
 */
public record JumpMetrics(Vec3 takeoff, Vec3 landing, float apexY, float airTime) {

    public JumpMetrics {
        takeoff = takeoff.copy();
        landing = landing.copy();
    }

    @Override
    public Vec3 takeoff() {
        return takeoff.copy();
    }

    @Override
    public Vec3 landing() {
        return landing.copy();
    }

    /** Ground covered, ignoring height. */
    public float distance() {
        float dx = landing.getX() - takeoff.getX();
        float dz = landing.getZ() - takeoff.getZ();
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** How far the feet rose above the take-off point. */
    public float rise() {
        return apexY - takeoff.getY();
    }

    /** Landing height relative to take-off: positive when landing higher. */
    public float heightChange() {
        return landing.getY() - takeoff.getY();
    }

    public String describe() {
        return String.format(Locale.ROOT,
            "%.2f m across, %.2f m up, landed %+.2f m, %.2f s in the air",
            distance(), rise(), heightChange(), airTime);
    }
}
