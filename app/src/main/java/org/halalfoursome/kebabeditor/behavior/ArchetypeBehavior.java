package org.halalfoursome.kebabeditor.behavior;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.utils.Maybe;

public interface ArchetypeBehavior {
    
    List<Drawable> drawableObjects(
        SceneNode node,
        ArchetypeInstance archetype,
        ModelResolver resolver
    );

    Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node,
        ArchetypeInstance archetype
    );

    Maybe<Aabb> bounds(
        SceneNode node,
        ArchetypeInstance archetype
    );

    List<BehaviorProblem> validate(
        SceneNode node,
        ArchetypeInstance archetype
    );
}
