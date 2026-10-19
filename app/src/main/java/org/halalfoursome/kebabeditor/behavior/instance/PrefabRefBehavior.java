package org.halalfoursome.kebabeditor.behavior.instance;

import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.archetype.instance.NodeArchetype;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.behavior.PlainBehaviorProblem;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.BrokenPrefab;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.LoadedPrefab;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

public class PrefabRefBehavior implements ArchetypeBehavior {

    private static final String PATH = "path";

    // What this node's path leads to once checked: 
    // a prefab we can draw (Open) 
    // or 
    // a reason we cannot (Blocked)
    private sealed interface Target {

        // The loaded prefab plus the context to draw its contents in
        record Open(LoadedPrefab prefab, BehaviorContext context) implements Target {}

        // Nothing is drawn from the prefab: 
        // the file is missing or unreadable, it contains itself, or nesting is too deep
        record Blocked(String detail) implements Target {}
    }

    @Override
    public List<Drawable> drawableObjects(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        Mat4 world = node.worldMatrix();

        return switch (target(archetype, context)) {
            case Target.Open open -> innerDrawableObjects(open, world);
            case Target.Blocked blocked -> List.of(Placeholders.brokenCube(world, blocked.detail()));
        };
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return Maybe.some(switch (target(archetype, context)) {
            case Target.Open open -> contentsBounds(open)
                .<GizmoShape>map(GizmoShape.GizmoAabb::new)
                .unwrapOr(new GizmoShape.GizmoBox(Vec3.one()));
            case Target.Blocked _ -> new GizmoShape.GizmoBox(Vec3.one());
        });
    }

    @Override
    public Maybe<Aabb> bounds(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        Mat4 world = node.worldMatrix();

        return Maybe.some(switch (target(archetype, context)) {
            case Target.Open open -> contentsBounds(open)
                .map(box -> box.transformed(world))
                .unwrapOr(Placeholders.brokenBounds(world));
            case Target.Blocked _ -> Placeholders.brokenBounds(world);
        });
    }

    @Override
    public List<BehaviorProblem> validate(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        List<BehaviorProblem> problems = new ArrayList<>();

        if (node.getArchetypes().size() > 1) {
            problems.add(problem("prefab_ref: a prefab reference should be the only archetype on its node"));
        }

        if (target(archetype, context) instanceof Target.Blocked blocked) {
            problems.add(problem("prefab_ref: " + blocked.detail()));
        }

        return problems;
    }

    private static Target target(ArchetypeInstance archetype, BehaviorContext context) {
        String path = Params.text(archetype, PATH).unwrapOr("");

        return switch (context.prefabs().resolve(context.scene(), path)) {
            case BrokenPrefab broken -> new Target.Blocked(broken.detail());

            case LoadedPrefab prefab -> {
                if (context.isActive(prefab.path())) {
                    yield new Target.Blocked("Prefab contains itself: " + prefab.path().getFileName());
                }
                if (context.isTooDeep()) {
                    yield new Target.Blocked("Prefabs are nested deeper than " + BehaviorContext.MAX_PREFAB_DEPTH);
                }

                yield new Target.Open(prefab, context.within(prefab.path()));
            }
        };
    }

    // The prefab's nodes are positioned relative to its own root, so the referencing node's world matrix goes in front
    private static List<Drawable> innerDrawableObjects(Target.Open open, Mat4 base) {
        List<Drawable> drawableObjects = new ArrayList<>();

        forEachBehavior(open, (behavior, node, instance) -> {
            for (Drawable drawable : behavior.drawableObjects(node, instance, open.context())) {
                drawableObjects.add(new Drawable(
                    drawable.mesh(),
                    base.mul(drawable.world()),
                    drawable.material(),
                    drawable.mode()
                ));
            }
        });

        return drawableObjects;
    }

    // Stays in the prefab's own space; 
    // the caller applies the referencing node's transform
    private static Maybe<Aabb> contentsBounds(Target.Open open) {
        Aabb[] union = new Aabb[1];

        forEachBehavior(open, (behavior, node, instance) ->
            behavior.bounds(node, instance, open.context()).ifSome(box ->
                union[0] = union[0] == null ? box : union[0].union(box)
            )
        );

        return Maybe.ofNullable(union[0]);
    }

    @FunctionalInterface
    private interface BehaviorVisitor {
        void visit(ArchetypeBehavior behavior, SceneNode node, ArchetypeInstance instance);
    }

    // Archetypes the user defined have no built-in behavior, so they are skipped
    private static void forEachBehavior(Target.Open open, BehaviorVisitor visitor) {
        for (SceneNode node : open.prefab().scene().traverse().toList()) {
            for (NodeArchetype archetype : node.getArchetypes()) {
                if (archetype instanceof ArchetypeInstance instance) {
                    BuiltinBehaviors
                        .behaviorOf(instance.archetypeId())
                        .ifSome(behavior -> visitor.visit(behavior, node, instance));
                }
            }
        }
    }

    private static BehaviorProblem problem(String message) {
        return new PlainBehaviorProblem(message);
    }
}
