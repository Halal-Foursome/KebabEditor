package org.halalfoursome.kebabeditor.behavior.instance;

import java.util.List;

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
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

public class ModelRefBehavior implements ArchetypeBehavior {

    private static final String PATH = "path";

    @Override
    public List<Drawable> drawableObjects(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        Mat4 world = node.worldMatrix();

        return switch (resolve(archetype, context)) {
            case ResolvedModel model -> model.parts().stream()
                .map(part -> drawable(part, world))
                .toList();

            case BrokenReference broken -> List.of(Placeholders.brokenCube(world, broken.detail()));
        };
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return Maybe.some(switch (resolve(archetype, context)) {
            case ResolvedModel model -> new GizmoShape.GizmoAabb(model.bounds());
            case BrokenReference _   -> new GizmoShape.GizmoBox(Vec3.one());
        });
    }

    @Override
    public Maybe<Aabb> bounds(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        Aabb local = switch (resolve(archetype, context)) {
            case ResolvedModel model -> model.bounds();
            case BrokenReference _ -> Meshes.CUBE_TRIANGLES.bounds();
        };

        return Maybe.some(local.transformed(node.worldMatrix()));
    }

    @Override
    public List<BehaviorProblem> validate(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return switch (resolve(archetype, context)) {
            case BrokenReference broken -> List.of(new PlainBehaviorProblem("model_ref: " + broken.detail()));

            case ResolvedModel model when model.parts().isEmpty() ->
                List.of(new PlainBehaviorProblem("model_ref: " + model.path().getFileName() + " has nothing to draw"));

            case ResolvedModel _ -> List.of();
        };
    }

    private static ModelResult resolve(ArchetypeInstance archetype, BehaviorContext context) {
        String path = Params.text(archetype, PATH).unwrapOr("");

        return context.models().resolve(context.scene(), path);
    }

    private static Drawable drawable(ModelPart part, Mat4 world) {
        return new Drawable(part.mesh(), world.mul(part.transform()), part.material(), DrawMode.SOLID);
    }
}
