package org.halalfoursome.kebabeditor.ui.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.junit.jupiter.api.Test;

class PlaytestKeysTest {

    private static final float EPS = 1e-5f;

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertTrue(expected.approxEquals(actual, EPS), "expected " + expected + " but was " + actual);
    }

    @Test
    void nothingHeldStandsStill() {
        assertVec(Vec3.zero(), new PlaytestKeys().move(0f));
    }

    @Test
    void forwardIsWhereTheCameraLooks() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_W);

        // Yaw 0 looks along -Z; a quarter turn left looks along -X.
        assertVec(new Vec3(0, 0, -1), keys.move(0f));
        assertVec(new Vec3(-1, 0, 0), keys.move((float) (Math.PI / 2)));
    }

    @Test
    void arrowKeysWorkLikeWasd() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_RIGHT);
        assertVec(new Vec3(1, 0, 0), keys.move(0f));

        keys.release(KeyEvent.VK_RIGHT);
        keys.press(KeyEvent.VK_DOWN);
        assertVec(new Vec3(0, 0, 1), keys.move(0f));
    }

    @Test
    void diagonalIsNoFasterThanStraight() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_W);
        keys.press(KeyEvent.VK_D);

        Vec3 move = keys.move(0f);

        assertEquals(1f, move.length(), EPS);
        assertEquals(0f, move.getY(), EPS);
    }

    @Test
    void oppositeKeysCancel() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_A);
        keys.press(KeyEvent.VK_D);

        assertVec(Vec3.zero(), keys.move(0f));
    }

    @Test
    void jumpIsTakenOncePerPress() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_SPACE);
        keys.press(KeyEvent.VK_SPACE); // key repeat while held

        assertTrue(keys.takeJump());
        assertFalse(keys.takeJump());

        keys.release(KeyEvent.VK_SPACE);
        keys.press(KeyEvent.VK_SPACE);
        assertTrue(keys.takeJump());
    }

    @Test
    void clearForgetsHeldKeys() {
        PlaytestKeys keys = new PlaytestKeys();
        keys.press(KeyEvent.VK_W);

        keys.clear();

        assertVec(Vec3.zero(), keys.move(0f));
    }
}
