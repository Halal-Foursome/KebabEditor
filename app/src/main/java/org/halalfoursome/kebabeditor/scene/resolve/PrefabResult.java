package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;
import java.util.Objects;

import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResult.*;

public sealed interface PrefabResult
    permits
        LoadedPrefab,
        BrokenPrefab
{
    Path path();

    record LoadedPrefab(Path path, Scene scene) implements PrefabResult {

        public LoadedPrefab {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(scene, "scene");
        }
    }

    record BrokenPrefab(Path path, Reason reason, String detail) implements PrefabResult {

        public BrokenPrefab {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }
    }
}
