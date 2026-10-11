package org.halalfoursome.kebabeditor.behavior.instance;

import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.builtin.BuiltinArchetypes;
import org.halalfoursome.kebabeditor.archetype.instance.ArchetypeInstance;
import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.math.Quat;
import org.halalfoursome.kebabeditor.math.Transform;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.scene.model.SceneNode;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;

final class BehaviorTestSupport {

    static final float EPS = 1e-4f;

    // Resolves nothing, for behaviours that never ask for a model
    static final BehaviorContext CONTEXT = new BehaviorContext(
        new ModelResolver() {
            @Override
            public ModelResult resolve(Path scene, String relPath) {
                return new BrokenReference(scene, Reason.MISSING, "no models in this test");
            }

            @Override
            public boolean wouldCycle(Path from, Path to) {
                return false;
            }
        },
        Path.of("level.gltf").toAbsolutePath()
    );

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
