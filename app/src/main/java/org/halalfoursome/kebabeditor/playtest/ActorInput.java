package org.halalfoursome.kebabeditor.playtest;

import org.halalfoursome.kebabeditor.math.Vec3;

/**
 * What the player asks of the actor for one step.
 *
 * @param move        wished horizontal direction in world space, at most unit length;
 *                    zero stands still
 * @param jumpPressed the jump key went down since the previous step
 */
public record ActorInput(Vec3 move, boolean jumpPressed) {

    public static final ActorInput NONE = new ActorInput(Vec3.zero(), false);

    public ActorInput {
        move = move.copy();
    }

    @Override
    public Vec3 move() {
        return move.copy();
    }
}
