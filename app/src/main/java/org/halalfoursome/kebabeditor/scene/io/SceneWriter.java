package org.halalfoursome.kebabeditor.scene.io;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;

public interface SceneWriter {

    void write(Scene scene, Path file) throws SceneFormatException, IOException;
}
