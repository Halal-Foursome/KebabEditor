package org.halalfoursome.kebabeditor.playtest;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.playtest.physics.ConvexShape;
import org.halalfoursome.kebabeditor.playtest.physics.Ellipsoid;
import org.halalfoursome.kebabeditor.playtest.physics.OrientedBox;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsWorld.Collider;
import org.halalfoursome.kebabeditor.scene.model.Greybox;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.LoadedPrefab;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

/**
 * A snapshot of a scene as the playtest sees it, taken when the playtest starts,
 * so the scene itself is never touched by the simulation.
 *
 * Solid: greybox shapes, box and sphere colliders. Trigger volumes report the
 * actor entering them. Models are only visuals; give them a collider to make them
 * solid. Prefab contents count too, placed by the referencing node. Player starts
 * are only taken from the level itself, not from prefabs inside it.
 */
public record PlaytestLevel(PhysicsWorld world, List<PlayerStart> starts) {

    private static final String PREFAB_SEPARATOR = " > ";

    public PlaytestLevel {
        starts = List.copyOf(starts);
    }

    /**
     * @param context resolves prefab references; without it prefabs add nothing
     */
    public static PlaytestLevel build(Scene scene, Maybe<BehaviorContext> context) {
        Collector collector = new Collector();
        collector.collect(scene, Mat4.identity(), context, "", true);

        return new PlaytestLevel(new PhysicsWorld(collector.solids, collector.triggers), collector.starts);
    }

    private static final class Collector {

        final List<Collider> solids = new ArrayList<>();
        final List<Collider> triggers = new ArrayList<>();
        final List<PlayerStart> starts = new ArrayList<>();

        void collect(Scene scene, Mat4 base, Maybe<BehaviorContext> context, String prefix, boolean level) {
            for (SceneNode node : scene.traverse().toList()) {
                Mat4 world = base.mul(node.worldMatrix());
                String name = prefix + node.getName();

                Greybox.of(node).ifSome(greybox -> {
                    Mat4 sized = world.mul(Mat4.scaling(greybox.size()));
                    add(solids, name, switch (greybox.shape()) {
                        case BOX -> OrientedBox.of(sized).map(ConvexShape.class::cast);
                        case SPHERE -> Ellipsoid.of(sized).map(ConvexShape.class::cast);
                    });
                });

                for (NodeArchetype archetype : node.getArchetypes()) {
                    if (archetype instanceof ArchetypeInstance instance) {
                        collect(node, instance, world, name, context, level);
                    }
                }
            }
        }

        private void collect(
            SceneNode node,
            ArchetypeInstance instance,
            Mat4 world,
            String name,
            Maybe<BehaviorContext> context,
            boolean level
        ) {
            switch (instance.archetypeId()) {
                case BuiltinArchetypes.COLLIDER_BOX -> add(solids, name, box(instance, world));
                case BuiltinArchetypes.TRIGGER -> add(triggers, name, box(instance, world));
                case BuiltinArchetypes.COLLIDER_SPHERE -> add(solids, name, Params.number(instance, "radius")
                    .filter(Params::positive)
                    .andThen(radius -> Ellipsoid.of(world.mul(Mat4.scaling(2f * radius))))
                    .map(ConvexShape.class::cast));
                case BuiltinArchetypes.PREFAB_REF -> context.ifSome(c -> prefab(instance, world, name, c));
                case BuiltinArchetypes.PLAYER_START -> {
                    if (level) {
                        starts.add(new PlayerStart(
                            Maybe.some(node),
                            world.getTranslation(),
                            PlayerStart.yawOf(world),
                            ActorSettings.from(instance),
                            ActorSettings.problems(instance)
                        ));
                    }
                }
                default -> { }
            }
        }

        // Broken, self-containing or too deeply nested prefabs are skipped, as the viewport does.
        private void prefab(ArchetypeInstance instance, Mat4 world, String name, BehaviorContext context) {
            String path = Params.text(instance, "path").unwrapOr("");

            if (context.prefabs().resolve(context.scene(), path) instanceof LoadedPrefab prefab
                && !context.isActive(prefab.path())
                && !context.isTooDeep()
            ) {
                collect(prefab.scene(), world, Maybe.some(context.within(prefab.path())), name + PREFAB_SEPARATOR, false);
            }
        }

        private static Maybe<ConvexShape> box(ArchetypeInstance instance, Mat4 world) {
            return Params.vec3(instance, "size")
                .filter(Collector::positive)
                .andThen(size -> OrientedBox.of(world.mul(Mat4.scaling(size))))
                .map(ConvexShape.class::cast);
        }

        private static boolean positive(Vec3 size) {
            return Params.positive(size.getX()) && Params.positive(size.getY()) && Params.positive(size.getZ());
        }

        private static void add(List<Collider> into, String name, Maybe<ConvexShape> shape) {
            shape.ifSome(s -> into.add(new Collider(s, name)));
        }
    }
}
