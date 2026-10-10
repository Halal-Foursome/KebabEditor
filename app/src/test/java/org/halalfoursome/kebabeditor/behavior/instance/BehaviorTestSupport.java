package org.halalfoursome.kebabeditor.behavior.instance;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;

final class BehaviorTestSupport {

    static final float EPS = 1e-4f;

    private BehaviorTestSupport() {}

    static ArchetypeInstance instanceOf(String archetypeId) {
        return new ArchetypeInstance(BuiltinArchetypes.find(archetypeId).unwrap());
    }

    static SceneNode nodeAt(Vec3 translation, float scale) {
        SceneNode node = new SceneNode("node");
        node.setTransform(new Transform(translation, Quat.identity(), scale));

        return node;
    }
}
