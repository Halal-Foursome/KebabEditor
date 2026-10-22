package org.halalfoursome.kebabeditor.behavior;

import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsVolume;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.Maybe;

public interface ArchetypeBehavior {
    
    List<Drawable> drawableObjects(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    );

    Maybe<GizmoShape> gizmoSelectionShape(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    );

    Maybe<Aabb> bounds(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    );

    List<BehaviorProblem> validate(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    );

    /**
     * What the archetype adds to playtest physics, built from the same
     * parameters and matrix as its drawing. Most archetypes add nothing.
     */
    default List<PhysicsVolume> physicsVolumes(
        SceneNode node,
        ArchetypeInstance archetype,
        BehaviorContext context
    ) {
        return List.of();
    }
}
