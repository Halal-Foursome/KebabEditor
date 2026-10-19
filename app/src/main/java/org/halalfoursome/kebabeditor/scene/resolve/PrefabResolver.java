package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;

public interface PrefabResolver {

    PrefabResult resolve(Path scene, String relPath);

    boolean wouldCycle(Path from, Path to);

    void invalidateAll();
}
