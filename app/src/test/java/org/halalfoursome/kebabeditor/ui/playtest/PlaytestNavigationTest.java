package org.halalfoursome.kebabeditor.ui.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.Set;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

class PlaytestNavigationTest {

    private static final float EPS = 1e-5f;

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertTrue(expected.approxEquals(actual, EPS), "expected " + expected + " but was " + actual);
    }

    @Test
    void nothingHeldStandsStill() {
        assertVec(Vec3.zero(), PlaytestNavigation.moveDirection(Set.of(), 0f));
    }

    @Test
    void forwardIsWhereTheCameraLooks() {
        Set<Integer> keys = Set.of(KeyEvent.VK_W);

        // Yaw 0 looks along -Z; a quarter turn left looks along -X.
        assertVec(new Vec3(0, 0, -1), PlaytestNavigation.moveDirection(keys, 0f));
        assertVec(new Vec3(-1, 0, 0), PlaytestNavigation.moveDirection(keys, (float) (Math.PI / 2)));
    }

    @Test
    void arrowKeysWorkLikeWasd() {
        assertVec(new Vec3(1, 0, 0), PlaytestNavigation.moveDirection(Set.of(KeyEvent.VK_RIGHT), 0f));
        assertVec(new Vec3(0, 0, 1), PlaytestNavigation.moveDirection(Set.of(KeyEvent.VK_DOWN), 0f));
    }

    @Test
    void diagonalIsNoFasterThanStraight() {
        Vec3 move = PlaytestNavigation.moveDirection(Set.of(KeyEvent.VK_W, KeyEvent.VK_D), 0f);

        assertEquals(1f, move.length(), EPS);
        assertEquals(0f, move.getY(), EPS);
    }

    @Test
    void oppositeKeysCancel() {
        assertVec(Vec3.zero(), PlaytestNavigation.moveDirection(Set.of(KeyEvent.VK_A, KeyEvent.VK_D), 0f));
    }
}
