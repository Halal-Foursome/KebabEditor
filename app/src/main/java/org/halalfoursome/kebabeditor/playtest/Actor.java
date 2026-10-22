package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.Capsule;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * The playtest character: a capsule moved by input, gravity and contacts.
 *
 * Each step accelerates towards the wished velocity, applies gravity, then moves
 * in substeps short enough that the capsule cannot pass through thin platforms,
 * pushing it out of every solid it overlaps. Contacts whose normal points mostly
 * up count as ground; the actor is pushed straight up out of those, so it stands
 * still on slopes instead of sliding down them.
 */
public final class Actor {

    /** Surfaces at most about 45° steep are ground. */
    static final float GROUND_NORMAL_Y = 0.7f;
    static final float GROUND_ACCELERATION = 60f;
    static final float AIR_ACCELERATION = 18f;
    static final float MAX_FALL_SPEED = 50f;
    /** A jump still works this long after walking off an edge. */
    static final float COYOTE_TIME = 0.1f;
    /** A jump pressed this long before landing still happens on landing. */
    static final float JUMP_BUFFER_TIME = 0.12f;
    /** Walking down slopes and steps keeps the feet on surfaces up to this far below. */
    static final float GROUND_SNAP = 0.25f;
    /** Shorter flights (bumps, snapping misses) are not reported as landings. */
    static final float MIN_REPORTED_AIR_TIME = 0.05f;

    private static final int RESOLVE_ITERATIONS = 4;
    private static final int MAX_SUBSTEPS = 32;

    private final ActorSettings settings;
    private final Vec3 position;
    private final Vec3 previousPosition;
    private final Vec3 velocity = Vec3.zero();
    private float yaw;

    private boolean grounded;
    private float coyote;
    private float jumpBuffer;

    private float airTime;
    private Vec3 airStart;
    private boolean jumped;
    private float apexY;

    /**
     * @param feet where the bottom of the capsule starts
     * @param yaw  facing, with the same convention as the editor camera
     */
    public Actor(ActorSettings settings, Vec3 feet, float yaw) {
        this.settings = settings;
        this.position = feet.copy();
        this.previousPosition = feet.copy();
        this.yaw = yaw;
        this.airStart = feet.copy();
    }

    public ActorSettings settings() {
        return settings;
    }

    /** Where the feet are. */
    public Vec3 position() {
        return position.copy();
    }

    /** Where the feet were before the last step, for smooth drawing between steps. */
    public Vec3 previousPosition() {
        return previousPosition.copy();
    }

    public Vec3 velocity() {
        return velocity.copy();
    }

    public float yaw() {
        return yaw;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public Capsule capsule() {
        return Capsule.standing(position, settings.radius(), settings.height());
    }

    /** Move back to a start point, standing still and in the air until the next contact. */
    public void teleport(Vec3 feet, float yaw) {
        position.set(feet);
        previousPosition.set(feet);
        velocity.set(0, 0, 0);
        this.yaw = yaw;
        grounded = false;
        coyote = 0;
        jumpBuffer = 0;
        resetFlight();
    }

    public List<ActorEvent> step(ActorInput input, PhysicsWorld world, float dt) {
        List<ActorEvent> events = new ArrayList<>();
        previousPosition.set(position);

        boolean wasGrounded = grounded;
        coyote = wasGrounded ? COYOTE_TIME : Math.max(0, coyote - dt);
        jumpBuffer = input.jumpPressed() ? JUMP_BUFFER_TIME : Math.max(0, jumpBuffer - dt);

        accelerate(input.move(), wasGrounded, dt);

        boolean jumpedNow = false;
        if (jumpBuffer > 0 && coyote > 0) {
            velocity.setY(settings.jumpSpeed());
            jumpBuffer = 0;
            coyote = 0;
            jumpedNow = true;
            // A coyote jump starts a new flight from where the actor is now.
            resetFlight();
            jumped = true;
            events.add(new ActorEvent.Jumped(position));
        }

        velocity.setY(Math.max(velocity.getY() - settings.gravity() * dt, -MAX_FALL_SPEED));

        grounded = move(world, dt);
        if (!grounded && wasGrounded && !jumpedNow && velocity.getY() <= 0) {
            grounded = snapToGround(world);
        }

        track(events, dt);
        return events;
    }

    private void accelerate(Vec3 move, boolean onGround, float dt) {
        Vec3 wish = new Vec3(move.getX(), 0, move.getZ());
        if (wish.lengthSquared() > 1) {
            wish.normalizeLocal();
        }
        if (wish.lengthSquared() > 1e-6f) {
            yaw = (float) Math.atan2(-wish.getX(), -wish.getZ());
        }
        wish.mulLocal(settings.runSpeed());

        // Move the horizontal velocity towards the wish by at most one step of acceleration.
        Vec3 horizontal = new Vec3(velocity.getX(), 0, velocity.getZ());
        Vec3 change = wish.sub(horizontal);
        float maxChange = (onGround ? GROUND_ACCELERATION : AIR_ACCELERATION) * dt;
        if (change.length() > maxChange) {
            change.normalizeLocal().mulLocal(maxChange);
        }

        velocity.setX(velocity.getX() + change.getX());
        velocity.setZ(velocity.getZ() + change.getZ());
    }

    private boolean move(PhysicsWorld world, float dt) {
        float travel = velocity.length() * dt;
        int substeps = Math.clamp((int) Math.ceil(travel / (settings.radius() * 0.5f)), 1, MAX_SUBSTEPS);
        float slice = dt / substeps;
        boolean onGround = false;

        for (int i = 0; i < substeps; i++) {
            position.addLocal(velocity.mul(slice));
            onGround |= resolve(world);
        }

        return onGround;
    }

    // Probe down; keep the probe only if it found ground.
    private boolean snapToGround(PhysicsWorld world) {
        Vec3 before = position.copy();
        Vec3 velocityBefore = velocity.copy();
        position.addLocal(0, -GROUND_SNAP, 0);

        if (resolve(world)) {
            return true;
        }

        position.set(before);
        velocity.set(velocityBefore);
        return false;
    }

    /** Push out of every overlapping solid; true when one of them is ground. */
    private boolean resolve(PhysicsWorld world) {
        boolean onGround = false;

        for (int iteration = 0; iteration < RESOLVE_ITERATIONS; iteration++) {
            boolean touched = false;

            for (Collider solid : world.solidsNear(capsule().bounds())) {
                Maybe<Capsule.Contact> contact = capsule().contact(solid.shape());
                if (contact.isNone()) {
                    continue;
                }

                touched = true;
                Vec3 normal = contact.unwrap().normal();
                float depth = contact.unwrap().depth();

                if (normal.getY() >= GROUND_NORMAL_Y) {
                    position.addLocal(0, depth / normal.getY(), 0);
                    velocity.setY(Math.max(velocity.getY(), 0));
                    onGround = true;
                } else {
                    position.addLocal(normal.mul(depth));
                    float into = velocity.dot(normal);
                    if (into < 0) {
                        velocity.subLocal(normal.mul(into));
                    }
                }
            }

            if (!touched) {
                break;
            }
        }

        return onGround;
    }

    private void track(List<ActorEvent> events, float dt) {
        if (!grounded) {
            airTime += dt;
            apexY = Math.max(apexY, position.getY());
            return;
        }

        if (airTime >= MIN_REPORTED_AIR_TIME || jumped) {
            Maybe<JumpMetrics> jump = jumped
                ? Maybe.some(new JumpMetrics(airStart, position, apexY, airTime))
                : Maybe.none();
            events.add(new ActorEvent.Landed(position, airTime, airStart.getY() - position.getY(), jump));
        }

        resetFlight();
    }

    private void resetFlight() {
        airTime = 0;
        airStart = position.copy();
        apexY = position.getY();
        jumped = false;
    }
}
