package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.behavior.PlainBehaviorProblem;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.MeshData;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.playtest.ActorSettings;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

/**
 * Draws the actor a playtest would spawn: its capsule standing on the node, at
 * the size its parameters give, and an arrow for the direction it faces.
 *
 * The node's scale is ignored, since the actor's size comes from the parameters.
 * Invalid parameters draw the default actor and are reported by validation.
 */
public class PlayerStartBehavior implements ArchetypeBehavior {

    private static final Material MATERIAL = Material.solid(new Color(235, 90, 120));
    private static final int SEGMENTS = 24;
    private static final int CACHED_SIZES = 16;

    // Each size gets its own mesh; cached so the GPU cache sees the same instance every frame.
    private final Map<ActorSettings, MeshData> outlines = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ActorSettings, MeshData> eldest) {
            return size() > CACHED_SIZES;
        }
    };

    @Override
    public List<Drawable> drawableObjects(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        ActorSettings settings = settings(archetype);
        Mat4 world = unscaled(node);
        Mat4 arrow = world
            .mul(Mat4.translation(new Vec3(0, settings.height() * 0.5f, 0)))
            .mul(Mat4.scaling(settings.radius() * 3f));

        return List.of(
            new Drawable(outline(settings), world, MATERIAL, DrawMode.OVERLAY),
            new Drawable(Meshes.ARROW_LINES, arrow, MATERIAL, DrawMode.OVERLAY)
        );
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        // The outline is placed with the node's full matrix, so undo its scale here.
        float scale = scaleOf(node.worldMatrix());
        ActorSettings settings = settings(archetype);
        float r = settings.radius() / scale;

        return Maybe.some(new GizmoShape.GizmoAabb(new Aabb(-r, r, 0, settings.height() / scale, -r, r)));
    }

    @Override
    public Maybe<Aabb> bounds(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        ActorSettings settings = settings(archetype);
        float r = settings.radius();

        return Maybe.some(new Aabb(-r, r, 0, settings.height(), -r, r).transformed(unscaled(node)));
    }

    @Override
    public List<BehaviorProblem> validate(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return ActorSettings.problems(archetype).stream()
            .<BehaviorProblem>map(problem -> new PlainBehaviorProblem("player_start: " + problem))
            .toList();
    }

    private static ActorSettings settings(ArchetypeInstance archetype) {
        return ActorSettings.from(archetype).unwrapOr(ActorSettings.DEFAULT);
    }

    private MeshData outline(ActorSettings settings) {
        return outlines.computeIfAbsent(settings,
            s -> Meshes.capsuleLines(s.radius(), s.height(), SEGMENTS));
    }

    private static Mat4 unscaled(SceneNode node) {
        Mat4 world = node.worldMatrix();
        return world.mul(Mat4.scaling(1f / scaleOf(world)));
    }

    private static float scaleOf(Mat4 world) {
        float scale = world.transformDirection(Vec3.unitX()).length();
        return scale > 1e-6f ? scale : 1f;
    }
}
