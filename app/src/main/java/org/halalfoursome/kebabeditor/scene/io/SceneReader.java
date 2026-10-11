package org.halalfoursome.kebabeditor.scene.io;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.model.Scene;

public interface SceneReader {

    Scene read(Path file, ArchetypeCatalog catalog) throws SceneException, IOException;
}
