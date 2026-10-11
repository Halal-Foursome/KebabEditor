package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Path;

public interface ModelResolver {

    ModelResult resolve(Path scene, String relPath);

    boolean wouldCycle(Path from, Path to);
}
