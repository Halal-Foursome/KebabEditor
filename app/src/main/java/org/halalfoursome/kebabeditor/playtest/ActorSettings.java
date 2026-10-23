package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

/**
 * The actor's movement metrics, read from a {@code player_start} object so each
 * level can be tested with the character it is designed for.
 *
 * @param runSpeed   top horizontal speed, m/s
 * @param jumpHeight how high a jump lifts the feet, m
 * @param gravity    downward acceleration, m/s²
 * @param radius     capsule radius, m
 * @param height     capsule height from feet to head, m
 */
public record ActorSettings(float runSpeed, float jumpHeight, float gravity, float radius, float height) {

    public static final String RUN_SPEED = "run_speed";
    public static final String JUMP_HEIGHT = "jump_height";
    public static final String GRAVITY = "gravity";
    public static final String RADIUS = "radius";
    public static final String HEIGHT = "height";

    public static final ActorSettings DEFAULT = new ActorSettings(6f, 1.5f, 25f, 0.35f, 1.8f);

    /** Speed that lifts the feet exactly {@link #jumpHeight} under {@link #gravity}. */
    public float jumpSpeed() {
        return (float) Math.sqrt(2 * gravity * jumpHeight);
    }

    /** The settings on a player_start, or none when {@link #problems} finds any. */
    public static Maybe<ActorSettings> from(ArchetypeInstance archetype) {
        if (!problems(archetype).isEmpty()) {
            return Maybe.none();
        }

        return Maybe.some(new ActorSettings(
            value(archetype, RUN_SPEED),
            value(archetype, JUMP_HEIGHT),
            value(archetype, GRAVITY),
            value(archetype, RADIUS),
            value(archetype, HEIGHT)
        ));
    }

    public static List<String> problems(ArchetypeInstance archetype) {
        List<String> problems = new ArrayList<>();

        for (String name : List.of(RUN_SPEED, JUMP_HEIGHT, GRAVITY, RADIUS, HEIGHT)) {
            Maybe<Float> value = Params.number(archetype, name);

            if (value.isNone()) {
                problems.add("'" + name + "' is missing or not a Float");
            } else if (!Params.positive(value.unwrap())) {
                problems.add("'" + name + "' must be positive, got " + value.unwrap());
            }
        }

        if (problems.isEmpty() && value(archetype, HEIGHT) < 2 * value(archetype, RADIUS)) {
            problems.add("'height' must be at least twice 'radius', got "
                + value(archetype, HEIGHT) + " < 2 * " + value(archetype, RADIUS));
        }

        return problems;
    }

    private static float value(ArchetypeInstance archetype, String name) {
        return Params.number(archetype, name).unwrap();
    }
}
