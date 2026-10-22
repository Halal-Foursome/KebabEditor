package org.halalfoursome.kebabeditor.playtest;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.utils.Maybe;

/** Something that happened to the actor during one physics step. */
public sealed interface ActorEvent {

    record Jumped(Vec3 takeoff) implements ActorEvent {

        public Jumped {
            takeoff = takeoff.copy();
        }
    }

    /**
     * Back on the ground after {@code airTime} seconds. {@code jump} is none when
     * the actor walked off an edge instead of jumping.
     */
    record Landed(Vec3 at, float airTime, float dropHeight, Maybe<JumpMetrics> jump) implements ActorEvent {

        public Landed {
            at = at.copy();
        }
    }
}
