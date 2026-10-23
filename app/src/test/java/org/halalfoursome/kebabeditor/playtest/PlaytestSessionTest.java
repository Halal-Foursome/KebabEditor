package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.logging.LogEntry;
import org.halalfoursome.kebabeditor.logging.LogLevel;
import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.OrientedBox;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

class PlaytestSessionTest {

    private static Collider box(String name, Vec3 center, Vec3 size) {
        return new Collider(OrientedBox.of(Mat4.compose(center, Quat.identity(), size)).unwrap(), name);
    }

    private static final Collider FLOOR = box("floor", new Vec3(0, -0.5f, 0), new Vec3(10, 1, 10));

    private final LogService log = new LogService();

    private static PlayerStart startAt(Vec3 feet) {
        return new PlayerStart(new SceneNode("spawn"), feet, 0, Maybe.some(ActorSettings.DEFAULT), List.of());
    }

    private List<String> messages() {
        return log.entries().stream().map(LogEntry::message).toList();
    }

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
        PlaytestSession session = new PlaytestSession(world, startAt(Vec3.zero()), log);

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
        PlaytestSession session = new PlaytestSession(world, startAt(Vec3.zero()), log);

        play(session, new Vec3(1, 0, 0), 1.2f);
        PathRecording recording = session.finish();

        assertEquals(1, recording.triggerHits().size());
        assertTrue(recording.triggerHits().getFirst().getX() > 2f);
    }

    @Test
    void fallingOutOfTheLevelRespawnsAtTheStart() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlaytestSession session = new PlaytestSession(world, startAt(Vec3.zero()), log);

        // Run off the 10 m floor and keep falling.
        play(session, new Vec3(1, 0, 0), 1.5f);
        play(session, Vec3.zero(), 3f);

        assertEquals(1, session.getRespawns());
        PathRecording recording = session.finish();
        assertEquals(2, recording.runs().size());
    }

    @Test
    void stoppingRightAwayLeavesNothingToDraw() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlaytestSession session = new PlaytestSession(world, startAt(Vec3.zero()), log);

        assertTrue(session.finish().isEmpty());
    }

    @Test
    void invalidStartIsRejected() {
        PhysicsWorld world = new PhysicsWorld(List.of(FLOOR), List.of());
        PlayerStart broken = new PlayerStart(new SceneNode("broken"), Vec3.zero(), 0, Maybe.none(), List.of("'gravity' must be positive"));

        assertThrows(IllegalArgumentException.class, () -> new PlaytestSession(world, broken, log));
    }

    @Test
    void jumpsAndTriggersAreLogged() {
        Collider goal = new Collider(
            OrientedBox.of(Mat4.compose(new Vec3(3, 1, 0), Quat.identity(), new Vec3(1, 2, 4))).unwrap(), "goal");
        PlaytestSession session = new PlaytestSession(
            new PhysicsWorld(List.of(FLOOR), List.of(goal)), startAt(Vec3.zero()), log);

        play(session, Vec3.zero(), 0.2f);
        session.advance(0.016, Vec3.zero(), true);
        play(session, Vec3.zero(), 1f);
        play(session, new Vec3(1, 0, 0), 1f);

        List<String> messages = messages();
        assertEquals(2, messages.size(), messages.toString());
        assertTrue(messages.get(0).startsWith("Playtest jump 1: "), messages.get(0));
        assertTrue(messages.get(1).startsWith("Playtest trigger 'goal' entered at "), messages.get(1));
    }

    @Test
    void dropsAndRespawnsAreLogged() {
        Collider ledge = box("ledge", new Vec3(0, 1.5f, 0), new Vec3(2, 1, 2));
        PlaytestSession session = new PlaytestSession(
            new PhysicsWorld(List.of(FLOOR, ledge), List.of()), startAt(new Vec3(0, 2, 0)), log);

        // Walk off the 2 m high ledge onto the floor, then off the floor's edge.
        play(session, Vec3.zero(), 0.2f);
        play(session, new Vec3(1, 0, 0), 4f);

        // After the respawn it runs off the ledge again, so only the first two matter.
        List<String> messages = messages();
        assertTrue(messages.get(0).startsWith("Playtest drop: fell 2."), messages.get(0));
        assertEquals("Playtest: the actor fell out of the level and respawned at 'spawn'", messages.get(1));
        assertEquals(LogLevel.WARN, log.entries().get(1).level());
    }
}
