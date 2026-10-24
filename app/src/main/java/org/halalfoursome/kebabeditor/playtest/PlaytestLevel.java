package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.instance.BuiltinBehaviors;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsVolume;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * A snapshot of a scene's physics and player starts, taken when a playtest starts.
 * Greyboxes are solid unless they carry a trigger; archetypes add their behavior's {@code physicsVolumes}.
 */
public record PlaytestLevel(PhysicsWorld world, List<PlayerStart> starts) {

    public PlaytestLevel {
        starts = List.copyOf(starts);
    }

    public static PlaytestLevel build(Scene scene, BehaviorContext context) {
        List<PhysicsVolume> volumes = new ArrayList<>();
        List<PlayerStart> starts = new ArrayList<>();

        for (SceneNode node : scene.traverse().toList()) {
            volumes.addAll(volumesOf(node, context));

            for (NodeArchetype archetype : node.getArchetypes()) {
                if (archetype instanceof ArchetypeInstance instance
                    && instance.archetypeId().equals(BuiltinArchetypes.PLAYER_START)) {
                    starts.add(start(node, instance));
                }
            }
        }

        return new PlaytestLevel(PhysicsWorld.of(volumes), starts);
    }

    /** What one object adds to the level's physics, not counting its children. */
    public static List<PhysicsVolume> volumesOf(SceneNode node, BehaviorContext context) {
        List<PhysicsVolume> volumes = new ArrayList<>();

        if (!isTriggerZone(node)) {
            Greybox.of(node).ifSome(greybox -> volumes.add(greyboxVolume(node, greybox)));
        }

        for (NodeArchetype archetype : node.getArchetypes()) {
            if (archetype instanceof ArchetypeInstance instance) {
                BuiltinBehaviors.behaviorOf(instance.archetypeId()).ifSome(behavior ->
                    volumes.addAll(behavior.physicsVolumes(node, instance, context))
                );
            }
        }

        return volumes;
    }

    /**
     * Where a playtest begins: the selected player start if one is selected,
     * else the first in the level; none when the level has no player start.
     */
    public Maybe<PlayerStart> startFor(Maybe<SceneNode> selected) {
        for (PlayerStart start : starts) {
            if (selected.isSomeAnd(node -> node == start.node())) {
                return Maybe.some(start);
            }
        }
        return starts.isEmpty() ? Maybe.none() : Maybe.some(starts.getFirst());
    }

    /** The level cannot be played: there is nothing for the actor to stand on. */
    public boolean hasNothingToStandOn() {
        return world.solids().isEmpty();
    }

    // A greybox that carries a trigger is a zone to walk into, not a wall
    private static boolean isTriggerZone(SceneNode node) {
        return node.getArchetypes().stream().anyMatch(archetype ->
            archetype instanceof ArchetypeInstance instance
                && instance.archetypeId().equals(BuiltinArchetypes.TRIGGER)
        );
    }

    // Placed like GreyboxRenderer draws it: the unit shape stretched to its size on top of the node.
    private static PhysicsVolume greyboxVolume(SceneNode node, Greybox greybox) {
        PhysicsVolume.Shape shape = switch (greybox.shape()) {
            case BOX -> PhysicsVolume.Shape.BOX;
            case SPHERE -> PhysicsVolume.Shape.SPHERE;
        };
        Mat4 world = node.worldMatrix().mul(Mat4.scaling(greybox.size()));

        return new PhysicsVolume(node.getName(), shape, world, PhysicsVolume.Role.SOLID);
    }

    private static PlayerStart start(SceneNode node, ArchetypeInstance instance) {
        Mat4 world = node.worldMatrix();

        return new PlayerStart(
            node,
            world.getTranslation(),
            PlayerStart.yawOf(world),
            ActorSettings.from(instance),
            ActorSettings.problems(instance)
        );
    }
}
