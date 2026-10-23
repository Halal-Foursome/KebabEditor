package org.halalfoursome.kebabeditor.playtest;

import java.nio.file.Path;

import org.halalfoursome.kebabeditor.behavior.BehaviorContext;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResolver;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.BrokenPrefab;

public final class PhysicsTestSupport {

    /** Resolves nothing: models and prefabs are all missing. */
    public static final BehaviorContext NO_ASSETS = new BehaviorContext(
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
        new PrefabResolver() {
            @Override
            public PrefabResult resolve(Path scene, String relPath) {
                return new BrokenPrefab(scene, Reason.MISSING, "no prefabs in this test");
            }

            @Override
            public boolean wouldCycle(Path from, Path to) {
                return false;
            }

            @Override
            public void invalidateAll() {}
        },
        Path.of("level.gltf")
    );

    private PhysicsTestSupport() {}
}
