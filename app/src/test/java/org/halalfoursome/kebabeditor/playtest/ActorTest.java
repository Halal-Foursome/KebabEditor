package org.halalfoursome.kebabeditor.playtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.OrientedBox;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.junit.jupiter.api.Test;

class ActorTest {

    private static final float DT = PlaytestSession.STEP;
    private static final ActorSettings SETTINGS = ActorSettings.DEFAULT;

    /** A box whose top face is at {@code topY}. */
    private static Collider slab(Vec3 center, Vec3 size) {
        return new Collider(OrientedBox.of(Mat4.compose(center, Quat.identity(), size)).unwrap(), "slab");
    }

    private static PhysicsWorld floorWorld(Collider... extra) {
        List<Collider> solids = new ArrayList<>(List.of(slab(new Vec3(0, -0.5f, 0), new Vec3(200, 1, 200))));
        solids.addAll(List.of(extra));
        return new PhysicsWorld(solids, List.of());
    }

    private static List<ActorEvent> run(Actor actor, PhysicsWorld world, ActorInput input, float seconds) {
        List<ActorEvent> events = new ArrayList<>();
        for (int i = 0; i < Math.round(seconds / DT); i++) {
            events.addAll(actor.step(input, world, DT));
        }
        return events;
    }

    @Test
    void fallsAndComesToRestOnTheFloor() {
        Actor actor = new Actor(SETTINGS, new Vec3(0, 3, 0), 0);
        PhysicsWorld world = floorWorld();

        run(actor, world, ActorInput.NONE, 2f);

        assertTrue(actor.isGrounded());
        assertEquals(0f, actor.position().getY(), 0.01f);
        assertEquals(0f, actor.velocity().length(), 1e-3f);
    }

    @Test
    void runsAtItsRunSpeed() {
        Actor actor = new Actor(SETTINGS, Vec3.zero(), 0);
        PhysicsWorld world = floorWorld();
        run(actor, world, ActorInput.NONE, 0.2f);

        run(actor, world, new ActorInput(new Vec3(1, 0, 0), false), 1f);
        float before = actor.position().getX();
        run(actor, world, new ActorInput(new Vec3(1, 0, 0), false), 1f);

        assertEquals(SETTINGS.runSpeed(), actor.position().getX() - before, 0.05f);
        assertEquals(0f, actor.position().getY(), 0.01f);
        // Facing turns towards the run direction: +X is a yaw of -90°.
        assertEquals(-Math.PI / 2, actor.yaw(), 1e-3);
    }

    @Test
    void jumpReachesItsJumpHeightAndReportsTheLanding() {
        Actor actor = new Actor(SETTINGS, Vec3.zero(), 0);
        PhysicsWorld world = floorWorld();
        run(actor, world, ActorInput.NONE, 0.2f);

        List<ActorEvent> events = new ArrayList<>(actor.step(new ActorInput(Vec3.zero(), true), world, DT));
        events.addAll(run(actor, world, ActorInput.NONE, 2f));

        assertTrue(events.getFirst() instanceof ActorEvent.Jumped);
        ActorEvent.Landed landed = events.stream()
            .filter(ActorEvent.Landed.class::isInstance)
            .map(ActorEvent.Landed.class::cast)
            .findFirst()
            .orElseThrow();
        JumpMetrics jump = landed.jump().unwrap();

        assertEquals(SETTINGS.jumpHeight(), jump.rise(), 0.05f);
        assertEquals(0f, jump.distance(), 1e-3f);
        assertEquals(2 * SETTINGS.jumpSpeed() / SETTINGS.gravity(), jump.airTime(), 0.03f);
    }

    @Test
    void runningJumpCoversTheExpectedDistance() {
        Actor actor = new Actor(SETTINGS, Vec3.zero(), 0);
        PhysicsWorld world = floorWorld();
        ActorInput runX = new ActorInput(new Vec3(1, 0, 0), false);
        run(actor, world, runX, 1f);

        List<ActorEvent> events = new ArrayList<>(actor.step(new ActorInput(new Vec3(1, 0, 0), true), world, DT));
        events.addAll(run(actor, world, runX, 1.5f));

        JumpMetrics jump = events.stream()
            .filter(ActorEvent.Landed.class::isInstance)
            .map(e -> ((ActorEvent.Landed) e).jump())
            .filter(j -> j.isSome())
            .findFirst()
            .orElseThrow()
            .unwrap();
        float expected = SETTINGS.runSpeed() * 2 * SETTINGS.jumpSpeed() / SETTINGS.gravity();
        assertEquals(expected, jump.distance(), 0.15f);
    }

    @Test
    void fastFallDoesNotTunnelThroughAThinPlatform() {
        Collider platform = slab(new Vec3(0, -0.025f, 0), new Vec3(4, 0.05f, 4));
        PhysicsWorld world = new PhysicsWorld(List.of(platform), List.of());
        Actor actor = new Actor(SETTINGS, new Vec3(0, 60, 0), 0);

        run(actor, world, ActorInput.NONE, 5f);

        assertTrue(actor.isGrounded());
        assertEquals(0f, actor.position().getY(), 0.01f);
    }

    @Test
    void standsStillOnAWalkableSlope() {
        Quat tilt = Quat.fromAxisAngle(Vec3.unitZ(), (float) Math.toRadians(30));
        Collider ramp = new Collider(OrientedBox.of(Mat4.compose(Vec3.zero(), tilt, new Vec3(20, 1, 20))).unwrap(), "ramp");
        PhysicsWorld world = new PhysicsWorld(List.of(ramp), List.of());
        Actor actor = new Actor(SETTINGS, new Vec3(0, 2, 0), 0);

        run(actor, world, ActorInput.NONE, 1f);
        Vec3 settled = actor.position();
        run(actor, world, ActorInput.NONE, 2f);

        assertTrue(actor.isGrounded());
        assertTrue(settled.approxEquals(actor.position(), 0.01f), "slid from " + settled + " to " + actor.position());
    }

    @Test
    void wallBlocksRunning() {
        Collider wall = slab(new Vec3(3, 2, 0), new Vec3(1, 4, 10));
        PhysicsWorld world = floorWorld(wall);
        Actor actor = new Actor(SETTINGS, Vec3.zero(), 0);

        run(actor, world, new ActorInput(new Vec3(1, 0, 0), false), 3f);

        // The wall's near face is at x = 2.5; the capsule stops a radius before it.
        assertEquals(2.5f - SETTINGS.radius(), actor.position().getX(), 0.01f);
        assertTrue(actor.isGrounded());
    }

    @Test
    void coyoteTimeAllowsAJumpJustAfterWalkingOffAnEdge() {
        Collider ledge = slab(new Vec3(0, -0.5f, 0), new Vec3(2, 1, 2));
        PhysicsWorld world = new PhysicsWorld(List.of(ledge), List.of());
        Actor actor = new Actor(SETTINGS, new Vec3(0.9f, 0, 0), 0);
        run(actor, world, ActorInput.NONE, 0.2f);

        // Walk until the capsule no longer touches the ledge.
        ActorInput runX = new ActorInput(new Vec3(1, 0, 0), false);
        while (actor.isGrounded()) {
            actor.step(runX, world, DT);
        }
        assertFalse(actor.isGrounded());

        List<ActorEvent> events = actor.step(new ActorInput(new Vec3(1, 0, 0), true), world, DT);

        assertTrue(events.stream().anyMatch(ActorEvent.Jumped.class::isInstance));
        assertTrue(actor.velocity().getY() > 0);
    }

    @Test
    void walksDownGentleStepsWithoutLeavingTheGround() {
        // Steps 10 cm high, 50 cm deep, going down along +X.
        List<Collider> steps = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            steps.add(slab(new Vec3(i * 0.5f, -0.5f - i * 0.1f, 0), new Vec3(0.5f, 1, 4)));
        }
        PhysicsWorld world = new PhysicsWorld(steps, List.of());
        Actor actor = new Actor(SETTINGS, Vec3.zero(), 0);
        run(actor, world, ActorInput.NONE, 0.3f);

        List<ActorEvent> events = run(actor, world, new ActorInput(new Vec3(0.5f, 0, 0), false), 1f);

        assertTrue(events.stream().noneMatch(ActorEvent.Landed.class::isInstance), "left the ground: " + events);
        assertTrue(actor.position().getX() > 1f);
    }
}
