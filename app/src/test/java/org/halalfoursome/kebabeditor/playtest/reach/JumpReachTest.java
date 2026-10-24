package org.halalfoursome.kebabeditor.playtest.reach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class JumpReachTest {

    // jumpHeight 1.5, gravity 25: the jump speed is sqrt(75) and a flat jump lasts 2 * sqrt(75) / 25 s.
    private static final JumpReach REACH = new JumpReach(ActorSettings.DEFAULT);

    /** A 1 m deep slab whose top is at {@code top}, spanning {@code minX..maxX} and {@code -1..1} on Z. */
    static Platform slab(String name, float minX, float maxX, float top) {
        return new Platform(new SceneNode(name), name, new Aabb(minX, maxX, top - 1, top, -1, 1));
    }

    @Test
    void touchingPlatformsAtTheSameHeightAreAWalk() {
        assertEquals(Maybe.some(JumpReach.Move.WALK), REACH.between(slab("a", 0, 2, 0), slab("b", 2, 4, 0)));
    }

    @Test
    void aFlatJumpCoversRunSpeedTimesAirTime() {
        float airTime = 2 * (float) Math.sqrt(75) / 25;

        assertEquals(6 * airTime, REACH.runSpeedReach(0), 1e-4f);
    }

    @Test
    void jumpsUpToTheJumpHeightButNoHigher() {
        Platform floor = slab("floor", 0, 2, 0);

        assertEquals(Maybe.some(JumpReach.Move.JUMP), REACH.between(floor, slab("box", 2, 3, 1.5f)));
        assertTrue(REACH.between(floor, slab("wall", 2, 3, 1.6f)).isNone());
    }

    @Test
    void gapsOutOfReachCannotBeJumped() {
        Platform from = slab("from", 0, 2, 0);
        float reach = REACH.runSpeedReach(0) + 2 * REACH.edgeGrip();

        assertEquals(Maybe.some(JumpReach.Move.JUMP), REACH.between(from, slab("near", 2 + reach - 0.01f, 5 + reach, 0)));
        assertTrue(REACH.between(from, slab("far", 2 + reach + 0.01f, 5 + reach, 0)).isNone());
    }

    @Test
    void droppingDownReachesFartherThanJumpingUp() {
        assertTrue(REACH.runSpeedReach(-5) > REACH.runSpeedReach(0));
        assertTrue(REACH.runSpeedReach(1) < REACH.runSpeedReach(0));
    }

    @Test
    void movesAreOneWay() {
        Platform low = slab("low", 0, 2, 0);
        Platform high = slab("high", 2, 4, 3);

        assertEquals(Maybe.some(JumpReach.Move.JUMP), REACH.between(high, low));
        assertTrue(REACH.between(low, high).isNone());
    }
}
