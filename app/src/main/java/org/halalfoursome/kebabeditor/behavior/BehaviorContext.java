package org.halalfoursome.kebabeditor.behavior;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResolver;

public record BehaviorContext(
    ModelResolver models,
    PrefabResolver prefabs,
    Path scene,
    List<Path> prefabChain
) {

    public static final int MAX_PREFAB_DEPTH = 16;

    public BehaviorContext {
        Objects.requireNonNull(models, "models");
        Objects.requireNonNull(prefabs, "prefabs");
        Objects.requireNonNull(scene, "scene");
        scene = scene.toAbsolutePath().normalize();
        prefabChain = List.copyOf(prefabChain);
    }

    public BehaviorContext(ModelResolver models, PrefabResolver prefabs, Path scene) {
        this(models, prefabs, scene, List.of(scene.toAbsolutePath().normalize()));
    }

    public BehaviorContext within(Path prefabFile) {
        List<Path> chain = new ArrayList<>(prefabChain);
        chain.add(prefabFile.toAbsolutePath().normalize());

        return new BehaviorContext(models, prefabs, prefabFile, chain);
    }

    public boolean isActive(Path file) {
        return prefabChain.contains(file.toAbsolutePath().normalize());
    }

    public boolean isTooDeep() {
        return prefabChain.size() > MAX_PREFAB_DEPTH;
    }
}
