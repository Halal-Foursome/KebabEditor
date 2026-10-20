package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;

public interface ProjectWriter {

    Path createProject(Path dir, String name) throws IOException;

    void saveScene(Path path, Scene sceneData) throws SceneFormatException, IOException;

    ProjectAssets saveAssetScope(Path scene, Path root, List<String> folders, List<String> scenes)
        throws IOException;

    void saveArchetypes(Path scene, ArchetypeRegistry registry) throws IOException;
}
