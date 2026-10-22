package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.OrientedBox;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class PlaytestSessionTest {

    private static Collider box(String name, Vec3 center, Vec3 size) {
        return new Collider(OrientedBox.of(Mat4.compose(center, Quat.identity(), size)).unwrap(), name);
    }

    private static final Collider FLOOR = box("floor", new Vec3(0, -0.5f, 0), new Vec3(10, 1, 10));

    private static void play(PlaytestSession session, Vec3 move, float seconds) {
        // Uneven frame times, as the Swing timer delivers them.
        double[] frames = { 0.016, 0.017, 0.015, 0.033 };
        double played = 0;
        for (int i = 0; played < seconds; i++) {
            double frame = frames[i % frames.length];
            session.advance(frame, move, false);
            played += frame;
        }
    }

    @Test
    void recordsThePathAndJumps() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlaytestSession session = new PlaytestSession(world, PlayerStart.fallback(Vec3.zero(), 0), PlaytestListener.NONE);

        play(session, Vec3.zero(), 0.2f);
        session.advance(0.016, new Vec3(1, 0, 0), true);
        play(session, new Vec3(1, 0, 0), 1.5f);

        PathRecording recording = session.finish();

        assertFalse(recording.isEmpty());
        assertEquals(1, recording.jumps().size());
        assertEquals(1, recording.runs().size());
        assertTrue(recording.duration() > 1.5f);
        // Some samples on the ground, some in the air.
        PathRecording.Run run = recording.runs().getFirst();
        boolean sawGround = false;
        boolean sawAir = false;
        for (int i = 0; i < run.size(); i++) {
            sawGround |= run.isGrounded(i);
            sawAir |= !run.isGrounded(i);
        }
        assertTrue(sawGround && sawAir);
    }

    @Test
    void enteringATriggerIsRecordedOnce() {
        Collider goal = box("goal", new Vec3(3, 1, 0), new Vec3(1, 2, 4));
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of(goal));
        PlaytestSession session = new PlaytestSession(world, PlayerStart.fallback(Vec3.zero(), 0), PlaytestListener.NONE);

        play(session, new Vec3(1, 0, 0), 1.2f);
        PathRecording recording = session.finish();

        assertEquals(1, recording.triggerHits().size());
        assertTrue(recording.triggerHits().getFirst().getX() > 2f);
    }

    @Test
    void fallingOutOfTheLevelRespawnsAtTheStart() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlaytestSession session = new PlaytestSession(world, PlayerStart.fallback(Vec3.zero(), 0), PlaytestListener.NONE);

        // Run off the 10 m floor and keep falling.
        play(session, new Vec3(1, 0, 0), 1.5f);
        play(session, Vec3.zero(), 3f);

        assertEquals(1, session.respawns());
        PathRecording recording = session.finish();
        assertEquals(2, recording.runs().size());
    }

    @Test
    void stoppingRightAwayLeavesNothingToDraw() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlaytestSession session = new PlaytestSession(world, PlayerStart.fallback(Vec3.zero(), 0), PlaytestListener.NONE);

        assertTrue(session.finish().isEmpty());
    }

    @Test
    void invalidStartIsRejected() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlayerStart broken = new PlayerStart(Maybe.none(), Vec3.zero(), 0, Maybe.none(), List.of("'gravity' must be positive"));

        assertThrows(IllegalArgumentException.class, () -> new PlaytestSession(world, broken, PlaytestListener.NONE));
    }

    /** Remembers what the session reported, in order. */
    private static final class Heard implements PlaytestListener {

        final List<String> events = new ArrayList<>();

        @Override
        public void jumped(int number, JumpMetrics jump) {
            events.add("jump " + number);
        }

        @Override
        public void dropped(float height, float airTime) {
            events.add(String.format(Locale.ROOT, "drop %.1f", height));
        }

        @Override
        public void triggerEntered(String trigger, float time) {
            events.add("trigger " + trigger);
        }

        @Override
        public void respawned(String start) {
            events.add("respawn " + start);
        }
    }

    @Test
    void listenerHearsJumpsAndTriggers() {
        Collider goal = new Collider(
            OrientedBox.of(Mat4.compose(new Vec3(3, 1, 0), Quat.identity(), new Vec3(1, 2, 4))).unwrap(), "goal");
        Heard heard = new Heard();
        PlaytestSession session = new PlaytestSession(
            new PhysicsWorld(List.of(FLOOR), List.of(goal)), PlayerStart.fallback(Vec3.zero(), 0), heard);

        play(session, Vec3.zero(), 0.2f);
        session.advance(0.016, Vec3.zero(), true);
        play(session, Vec3.zero(), 1f);
        play(session, new Vec3(1, 0, 0), 1f);

        assertEquals(List.of("jump 1", "trigger goal"), heard.events);
    }

    @Test
    void listenerHearsDropsAndRespawns() {
        Collider ledge = box("ledge", new Vec3(0, 1.5f, 0), new Vec3(2, 1, 2));
        Heard heard = new Heard();
        PlaytestSession session = new PlaytestSession(
            new PhysicsWorld(List.of(FLOOR, ledge), List.of()), PlayerStart.fallback(new Vec3(0, 2, 0), 0), heard);

        // Walk off the 2 m high ledge onto the floor, then off the floor's edge.
        play(session, Vec3.zero(), 0.2f);
        play(session, new Vec3(1, 0, 0), 4f);

        // After the respawn it runs off the ledge again, so only the first two matter.
        assertEquals(List.of("drop 2.0", "respawn default start"), heard.events.subList(0, 2));
    }
}
