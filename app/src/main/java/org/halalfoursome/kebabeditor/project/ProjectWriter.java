package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;

public interface ProjectWriter {

    Path createProject(Path dir, String name) throws IOException;

    void saveScene(Path path, Scene sceneData) throws SceneFormatException, IOException;

    void saveArchetypes(Path scene, ArchetypeRegistry registry) throws IOException;
}
