package org.halalfoursome.kebabeditor.playtest;

import java.util.List;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Where a playtest can begin: a {@code player_start} object, or the fallback
 * the editor picks when the level has none.
 *
 * @param node     the player_start object; none for a fallback
 * @param feet     where the actor's feet start
 * @param yaw      starting facing, with the same convention as the editor camera
 * @param settings the actor's metrics; none when the object's parameters are invalid
 * @param problems why the settings are invalid
 */
public record PlayerStart(
    Maybe<SceneNode> node,
    Vec3 feet,
    float yaw,
    Maybe<ActorSettings> settings,
    List<String> problems
) {

    public PlayerStart {
        feet = feet.copy();
        problems = List.copyOf(problems);
    }

    /** A start with default metrics, for levels without a player_start. */
    public static PlayerStart fallback(Vec3 feet, float yaw) {
        return new PlayerStart(Maybe.none(), feet, yaw, Maybe.some(ActorSettings.DEFAULT), List.of());
    }

    @Override
    public Vec3 feet() {
        return feet.copy();
    }

    public String name() {
        return node.map(SceneNode::getName).unwrapOr("default start");
    }

    /** The facing a world matrix gives its local -Z axis, which the start marker's arrow shows. */
    public static float yawOf(Mat4 world) {
        Vec3 forward = world.transformDirection(new Vec3(0, 0, -1));
        if (forward.getX() * forward.getX() + forward.getZ() * forward.getZ() < 1e-8f) {
            return 0f;
        }
        return (float) Math.atan2(-forward.getX(), -forward.getZ());
    }
}
