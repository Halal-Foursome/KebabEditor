package org.halalfoursome.kebabeditor.behavior.instance;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.utils.Maybe;

public record ColliderBoxBehavior(
    
) implements ArchetypeBehavior {

    @Override
    public List<Drawable> drawableObjects(SceneNode node, ArchetypeInstance archetype, ModelResolver resolver) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'drawableObjects'");
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(SceneNode node, ArchetypeInstance archetype) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'gizmoSelectionShape'");
    }

    @Override
    public Maybe<Aabb> bounds(SceneNode node, ArchetypeInstance archetype) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bounds'");
    }

    @Override
    public List<BehaviorProblem> validate(SceneNode node, ArchetypeInstance archetype) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'validate'");
    }
}
