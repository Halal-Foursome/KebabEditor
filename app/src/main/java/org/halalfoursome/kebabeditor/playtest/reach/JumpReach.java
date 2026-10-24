package org.halalfoursome.kebabeditor.playtest.reach;

import org.halalfoursome.kebabeditor.playtest.Actor;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * How the actor gets from the top of one platform onto another, worked out from
 * {@link ActorSettings} instead of simulated: a jump at full run speed from the
 * nearest edges, landing on the way down. What lies between the platforms
 * (walls, ceilings) is not checked, so this is the best case a player can do.
 */
public record JumpReach(ActorSettings settings) {

    public enum Move {
        /** The platforms touch at about the same height: no jump needed. */
        WALK,
        /** A jump, or a drop off the edge. */
        JUMP,
    }

    /** The move that gets the actor from {@code from} onto {@code to}, or none when it is out of reach. */
    public Maybe<Move> between(Platform from, Platform to) {
        float rise = to.top() - from.top();
        float gap = Math.max(0, from.gap(to) - 2 * edgeGrip());

        if (gap == 0 && rise >= -Actor.GROUND_SNAP && rise <= stepHeight()) {
            return Maybe.some(Move.WALK);
        }
        if (rise > settings.jumpHeight()) {
            return Maybe.none();
        }

        return gap <= runSpeedReach(rise) ? Maybe.some(Move.JUMP) : Maybe.none();
    }

    /**
     * Horizontal distance covered by a running jump that lands {@code rise} above
     * the take-off height: run speed times the time until the feet come back down to it.
     */
    public float runSpeedReach(float rise) {
        float speed = settings.jumpSpeed();
        float gravity = settings.gravity();
        float airTime = (speed + (float) Math.sqrt(Math.max(0, speed * speed - 2 * gravity * rise))) / gravity;
        return settings.runSpeed() * airTime;
    }

    /**
     * How far past an edge the feet can be while the capsule still stands on it:
     * the edge must touch the rounded bottom where it is flat enough to be ground.
     */
    float edgeGrip() {
        float normalY = Actor.GROUND_NORMAL_Y;
        return settings.radius() * (float) Math.sqrt(1 - normalY * normalY);
    }

    // The rounded bottom climbs a ledge only while the contact there still counts as ground.
    float stepHeight() {
        return settings.radius() * (1 - Actor.GROUND_NORMAL_Y);
    }
}
