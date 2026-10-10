package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.DrawMode;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.behavior.PlainBehaviorProblem;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.utils.Maybe;
import org.halalfoursome.kebabeditor.utils.Params;

public class ColliderSphereBehavior implements ArchetypeBehavior {

    private static final String RADIUS = "radius";
    private static final Material MATERIAL = Material.solid(new Color(80, 220, 120, 200));

    @Override
    public List<Drawable> drawableObjects(
        SceneNode node, 
        ArchetypeInstance archetype, 
        ModelResolver resolver
    ) {
        return validRadius(archetype)
            .map(radius -> List.of(
                new Drawable(
                    Meshes.SPHERE_LINES,
                    node.worldMatrix().mul(Mat4.scaling(2f * radius)),
                    MATERIAL,
                    DrawMode.OVERLAY
                )
            ))
            .unwrapOr(List.of());
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node, 
        ArchetypeInstance archetype
    ) {
        return validRadius(archetype)
            .map(GizmoShape.GizmoSphere::new);
    }

    @Override
    public Maybe<Aabb> bounds(SceneNode node, ArchetypeInstance archetype) {
        return validRadius(archetype).map(radius -> {
            Mat4 world = node.worldMatrix();
            float scale = world.transformDirection(Vec3.unitX()).length();

            return Aabb.centered(world.getTranslation(), radius * scale);
        });
    }

    @Override
    public List<BehaviorProblem> validate(SceneNode node, ArchetypeInstance archetype) {
        Maybe<Float> radius = Params.number(archetype, RADIUS);

        if (radius.isNone()) {
            return List.of(new PlainBehaviorProblem("collider_sphere: 'radius' is missing or not a Float"));
        }

        if (validRadius(archetype).isNone()) {
            return List.of(new PlainBehaviorProblem(
                "collider_sphere: radius must be positive, got " + radius.unwrap()
            ));
        }

        return List.of();
    }

    private static Maybe<Float> validRadius(ArchetypeInstance archetype) {
        return Params.number(archetype, RADIUS).
            filter(Params::positive);
    }
}
