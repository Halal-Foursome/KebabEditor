package org.halalfoursome.kebabeditor.behavior.instance;

import java.awt.Color;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.ArchetypeBehavior;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.behavior.BehaviorProblem;
import org.halalfoursome.kebabeditor.behavior.Drawable;
import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.model.GizmoShape;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.playtest.physics.PhysicsVolume;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.utils.BoxVolumes;
import org.halalfoursome.kebabeditor.utils.Maybe;

public class TriggerBehavior implements ArchetypeBehavior {

    private static final Material MATERIAL = Material.solid(new Color(255, 170, 60, 200));

    @Override
    public List<Drawable> drawableObjects(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return BoxVolumes.drawableObjects(node, archetype, MATERIAL);
    }

    @Override
    public Maybe<GizmoShape> gizmoSelectionShape(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return BoxVolumes.selectionShape(archetype);
    }

    @Override
    public Maybe<Aabb> bounds(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return BoxVolumes.bounds(node, archetype);
    }

    @Override
    public List<BehaviorProblem> validate(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return BoxVolumes.validate("trigger", archetype);
    }

    @Override
    public List<PhysicsVolume> physicsVolumes(SceneNode node, ArchetypeInstance archetype, BehaviorContext context) {
        return BoxVolumes.physicsVolumes(node, archetype, PhysicsVolume.Role.TRIGGER);
    }
}
