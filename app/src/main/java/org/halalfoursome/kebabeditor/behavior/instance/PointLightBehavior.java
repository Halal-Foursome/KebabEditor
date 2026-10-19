package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class PointLightBehavior implements ArchetypeBehavior {

    private static final String LABEL = "point_light";

    @Override
    public List<Drawable> drawableObjects(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return gizmos(node.worldMatrix(), archetype);
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return Maybe.some(new GizmoShape.GizmoSphere(LightGizmos.MARKER_DIAMETER / 2f));
    }

    @Override
    public Maybe<Aabb> bounds(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return LightGizmos.boundsOf(gizmos(node.worldMatrix(), archetype));
    }

    @Override
    public List<BehaviorProblem> validate(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        List<BehaviorProblem> problems = new ArrayList<>(LightGizmos.validateIntensity(LABEL, archetype));
        problems.addAll(LightGizmos.validateRange(LABEL, archetype));

        return problems;
    }

    private static List<Drawable> gizmos(Mat4 world, ArchetypeInstance archetype) {
        Color color = LightGizmos.colorOf(archetype);
        List<Drawable> gizmos = new ArrayList<>();

        gizmos.add(LightGizmos.marker(world, color));
        LightGizmos.validRange(archetype)
            .ifSome(range -> gizmos.add(LightGizmos.rangeSphere(world, color, range)));

        return gizmos;
    }
}
