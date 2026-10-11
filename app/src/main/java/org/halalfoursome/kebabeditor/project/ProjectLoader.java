package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.scene.error.SceneException;

public interface ProjectLoader {

    ProjectRepository load(Path file) throws SceneException, IOException;
}
