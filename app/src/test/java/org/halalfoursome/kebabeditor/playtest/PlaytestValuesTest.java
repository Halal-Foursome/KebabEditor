package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.junit.jupiter.api.Test;

/** The small value types of a playtest: settings, starts, jumps and the summary. */
class PlaytestValuesTest {

    private static final float EPS = 1e-4f;

    private static ArchetypeInstance playerStart() {
        return new ArchetypeInstance(BuiltinArchetypes.find(BuiltinArchetypes.PLAYER_START).unwrap());
    }

    @Test
    void defaultPlayerStartGivesTheDefaultSettings() {
        assertEquals(ActorSettings.DEFAULT, ActorSettings.from(playerStart()).unwrap());
        assertTrue(ActorSettings.problems(playerStart()).isEmpty());
    }

    @Test
    void jumpSpeedReachesTheJumpHeight() {
        ActorSettings settings = new ActorSettings(6, 2, 20, 0.3f, 1.8f);

        // v^2 = 2 g h
        assertEquals(Math.sqrt(2 * 20 * 2), settings.jumpSpeed(), EPS);
    }

    @Test
    void everyNonPositiveMetricIsAProblem() throws Exception {
        ArchetypeInstance start = playerStart();
        start.set(ActorSettings.RUN_SPEED, 0f);
        start.set(ActorSettings.GRAVITY, Float.NaN);

        assertEquals(2, ActorSettings.problems(start).size());
        assertTrue(ActorSettings.from(start).isNone());
    }

    @Test
    void capsuleShorterThanItsWidthIsAProblem() throws Exception {
        ArchetypeInstance start = playerStart();
        start.set(ActorSettings.RADIUS, 0.5f);
        start.set(ActorSettings.HEIGHT, 0.9f);

        List<String> problems = ActorSettings.problems(start);

        assertEquals(1, problems.size());
        assertTrue(problems.getFirst().contains("twice"), problems.toString());
    }

    @Test
    void yawFollowsTheLocalMinusZAxis() {
        assertEquals(0f, PlayerStart.yawOf(Mat4.identity()), EPS);
        assertEquals(Math.PI / 2, PlayerStart.yawOf(Mat4.rotation(Quat.fromAxisAngle(Vec3.unitY(), (float) (Math.PI / 2)))), EPS);
        // Facing straight down has no horizontal direction, so it falls back to 0.
        assertEquals(0f, PlayerStart.yawOf(Mat4.rotation(Quat.fromAxisAngle(Vec3.unitX(), (float) (-Math.PI / 2)))), EPS);
    }

    @Test
    void startIsNamedAfterItsNodeAndKeepsItsFeet() {
        Vec3 feet = new Vec3(1, 2, 3);
        PlayerStart start = new PlayerStart(new SceneNode("spawn"), feet, 0.5f, Maybe.some(ActorSettings.DEFAULT), List.of());

        feet.set(9, 9, 9);
        start.feet().set(9, 9, 9);

        assertEquals("spawn", start.name());
        assertTrue(new Vec3(1, 2, 3).approxEquals(start.feet(), EPS));
    }

    @Test
    void jumpMetricsMeasureAcrossUpAndChange() {
        JumpMetrics jump = new JumpMetrics(new Vec3(0, 1, 0), new Vec3(3, 1.5f, 4), 2.5f, 0.6f);

        assertEquals(5f, jump.distance(), EPS);
        assertEquals(1.5f, jump.rise(), EPS);
        assertEquals(0.5f, jump.heightChange(), EPS);
        assertEquals("5.00 m across, 1.50 m up, landed +0.50 m, 0.60 s in the air", jump.describe());
    }

    @Test
    void jumpMetricsDoNotShareTheirVectors() {
        Vec3 takeoff = Vec3.zero();
        JumpMetrics jump = new JumpMetrics(takeoff, Vec3.one(), 1, 1);

        takeoff.set(9, 9, 9);
        jump.landing().set(9, 9, 9);

        assertTrue(Vec3.zero().approxEquals(jump.takeoff(), EPS));
        assertTrue(Vec3.one().approxEquals(jump.landing(), EPS));
    }

    @Test
    void summaryNamesTheLongestAndHighestJumps() {
        List<JumpMetrics> jumps = List.of(
            new JumpMetrics(Vec3.zero(), new Vec3(2, 0, 0), 1.5f, 0.5f),
            new JumpMetrics(Vec3.zero(), new Vec3(4, 0, 0), 1.0f, 0.5f)
        );

        assertEquals(
            "Playtest stopped after 12.3 s: 2 jump(s), longest 4.00 m, highest 1.50 m, 1 fall(s) out of the level",
            PlaytestSummary.describe(12.34f, jumps, 1)
        );
        assertEquals("Playtest stopped after 0.5 s: 0 jump(s)", PlaytestSummary.describe(0.5f, List.of(), 0));
    }
}
