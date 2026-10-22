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
 * A snapshot of a scene as the playtest sees it, taken when the playtest starts,
 * so the scene itself is never touched by the simulation.
 *
 * Greyboxes are solid. Archetypes add what their behavior's
 * {@code physicsVolumes} gives, built from the same parameters they are drawn
 * with, so what collides is what the viewport shows: box and sphere colliders
 * are solid, triggers report the actor entering them, and prefab references
 * bring their contents' archetypes along. Models are only visuals; give them a
 * collider to make them solid.
 *
 * Player starts are only taken from the level itself, not from prefabs inside it.
 */
public record PlaytestLevel(PhysicsWorld world, List<PlayerStart> starts) {

    public PlaytestLevel {
        starts = List.copyOf(starts);
    }

    public static PlaytestLevel build(Scene scene, BehaviorContext context) {
        List<PhysicsVolume> volumes = new ArrayList<>();
        List<PlayerStart> starts = new ArrayList<>();

        for (SceneNode node : scene.traverse().toList()) {
            Greybox.of(node).ifSome(greybox -> volumes.add(greyboxVolume(node, greybox)));

            for (NodeArchetype archetype : node.getArchetypes()) {
                if (!(archetype instanceof ArchetypeInstance instance)) {
                    continue;
                }

                BuiltinBehaviors.behaviorOf(instance.archetypeId()).ifSome(behavior ->
                    volumes.addAll(behavior.physicsVolumes(node, instance, context))
                );

                if (instance.archetypeId().equals(BuiltinArchetypes.PLAYER_START)) {
                    starts.add(start(node, instance));
                }
            }
        }

        return new PlaytestLevel(PhysicsWorld.of(volumes), starts);
    }

    /**
     * Where a playtest begins: the selected player start if one is selected,
     * else the first in the level; none when the level has no player start.
     */
    public Maybe<PlayerStart> startFor(Maybe<SceneNode> selected) {
        for (PlayerStart start : starts) {
            if (start.node().isSome() && selected.isSomeAnd(node -> node == start.node().unwrap())) {
                return Maybe.some(start);
            }
        }
        return starts.isEmpty() ? Maybe.none() : Maybe.some(starts.getFirst());
    }

    /** The level cannot be played: there is nothing for the actor to stand on. */
    public boolean hasNothingToStandOn() {
        return world.solids().isEmpty();
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
            Maybe.some(node),
            world.getTranslation(),
            PlayerStart.yawOf(world),
            ActorSettings.from(instance),
            ActorSettings.problems(instance)
        );
    }
}
