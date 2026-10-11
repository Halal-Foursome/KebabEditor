package org.halalfoursome.kebabeditor.behavior;

import java.nio.file.Path;
import java.util.Objects;

import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;

public record BehaviorContext(
    ModelResolver models, 
    Path scene
) {

    public BehaviorContext {
        Objects.requireNonNull(models, "models");
        Objects.requireNonNull(scene, "scene");
    }
}
