package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lombok.NoArgsConstructor;

@NoArgsConstructor 
public class ProjectWriter {

    public Path createProject(Path dir, String name) throws IOException {
        Files.createDirectories(dir);

        Path scene = dir.resolve(name + ".gltf");
        Files.writeString(scene, ProjectTemplates.MINIMAL_GLTF, StandardOpenOption.CREATE_NEW);

        // Note: one archetypes file is shared by all scenes in the same folder
        Path archetypes = dir.resolve(ProjectTemplates.ARCHETYPES_FILE);
        if (!Files.exists(archetypes)) {
            Files.writeString(archetypes, ProjectTemplates.EMPTY_ARCHETYPES);
        }

        return scene;
    }
}
