package org.halalfoursome.kebabeditor.playtest;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.halalfoursome.kebabeditor.logging.LogService;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;

/**
 * One running playtest: the actor in a fixed world, stepped at a fixed rate so
 * a run does not depend on the frame rate, and recorded for the path rays.
 *
 * Jumps and longer drops are logged with their metrics as they land; entering
 * a trigger is logged too. Falling far below the level respawns the actor.
 */
public final class PlaytestSession {

    public static final float STEP = 1f / 120f;

    /** A stalled frame does not try to catch up on more than this much time. */
    private static final double MAX_FRAME_SECONDS = 0.25;
    /** Path samples are taken every few steps, and whenever the actor lands or leaves the ground. */
    private static final int SAMPLE_EVERY_STEPS = 4;
    /** Falling this far below the lowest solid counts as falling out of the level. */
    private static final float FALL_LIMIT = 25f;
    /** Walking off ledges lower than this is not worth a log line. */
    private static final float MIN_REPORTED_DROP = 1f;

    private final PhysicsWorld world;
    private final PlayerStart start;
    private final Actor actor;
    private final PathRecorder recorder = new PathRecorder();
    private final Set<Collider> insideTriggers = Collections.newSetFromMap(new IdentityHashMap<>());
    private final float killY;

    private double accumulator;
    private int steps;
    private boolean jumpQueued;
    private boolean wasGrounded;
    private int respawns;

    /**
     * @throws IllegalArgumentException when the start's settings are invalid
     */
    public PlaytestSession(PhysicsWorld world, PlayerStart start) {
        ActorSettings settings = start.settings().unwrapOrElse(() -> {
            throw new IllegalArgumentException(
                "Player start '" + start.name() + "' is invalid: " + String.join("; ", start.problems())
            );
        });

        this.world = world;
        this.start = start;
        this.actor = new Actor(settings, start.feet(), start.yaw());

        float lowest = world.solidBounds().map(Aabb::getMinY).unwrapOr(start.feet().getY());
        this.killY = Math.min(lowest, start.feet().getY()) - FALL_LIMIT;

        recorder.sample(start.feet(), false);
    }

    /**
     * Run the simulation for one frame of wall-clock time.
     *
     * @param move        wished horizontal direction in world space, at most unit length
     * @param jumpPressed the jump key went down during this frame
     */
    public void advance(double seconds, Vec3 move, boolean jumpPressed) {
        jumpQueued |= jumpPressed;
        accumulator += Math.clamp(seconds, 0, MAX_FRAME_SECONDS);

        while (accumulator >= STEP) {
            accumulator -= STEP;
            step(new ActorInput(move, jumpQueued));
            jumpQueued = false;
        }
    }

    /** The feet, smoothed between the last two steps for drawing. */
    public Vec3 actorPosition() {
        return actor.previousPosition().lerpLocal(actor.position(), (float) (accumulator / STEP));
    }

    public float actorYaw() {
        return actor.yaw();
    }

    public ActorSettings settings() {
        return actor.settings();
    }

    public PhysicsWorld world() {
        return world;
    }

    public PlayerStart start() {
        return start;
    }

    public PathRecorder recorder() {
        return recorder;
    }

    /** Simulated seconds so far. */
    public float elapsed() {
        return steps * STEP;
    }

    public List<JumpMetrics> jumps() {
        return recorder.jumps();
    }

    public int respawns() {
        return respawns;
    }

    /** The path so far, ending where the actor is now. */
    public PathRecording finish() {
        recorder.sample(actor.position(), actor.isGrounded());
        return recorder.snapshot(elapsed());
    }

    private void step(ActorInput input) {
        for (ActorEvent event : actor.step(input, world, STEP)) {
            if (event instanceof ActorEvent.Landed landed) {
                landed(landed);
            }
        }
        steps++;

        updateTriggers();

        if (actor.position().getY() < killY) {
            respawn();
        }

        boolean grounded = actor.isGrounded();
        if (steps % SAMPLE_EVERY_STEPS == 0 || grounded != wasGrounded) {
            recorder.sample(actor.position(), grounded);
        }
        wasGrounded = grounded;
    }

    private void landed(ActorEvent.Landed landed) {
        landed.jump().ifSomeOrElse(jump -> {
            recorder.addJump(jump);
            LogService.get().info("Playtest jump " + recorder.jumps().size() + ": " + jump.describe());
        }, () -> {
            if (landed.dropHeight() >= MIN_REPORTED_DROP) {
                LogService.get().info(String.format(Locale.ROOT,
                    "Playtest drop: fell %.2f m, %.2f s in the air", landed.dropHeight(), landed.airTime()));
            }
        });
    }

    private void updateTriggers() {
        List<Collider> touching = world.triggersTouching(actor.capsule());

        for (Collider trigger : touching) {
            if (!insideTriggers.contains(trigger)) {
                recorder.addTriggerHit(actor.position());
                LogService.get().info(String.format(Locale.ROOT,
                    "Playtest trigger '%s' entered at %.2f s", trigger.name(), elapsed()));
            }
        }

        insideTriggers.clear();
        insideTriggers.addAll(touching);
    }

    private void respawn() {
        respawns++;
        LogService.get().warn("Playtest: the actor fell out of the level and respawned at '" + start.name() + "'");

        recorder.sample(actor.position(), false);
        recorder.breakPath();
        actor.teleport(start.feet(), start.yaw());
        insideTriggers.clear();
        recorder.sample(start.feet(), false);
        wasGrounded = false;
    }
}
