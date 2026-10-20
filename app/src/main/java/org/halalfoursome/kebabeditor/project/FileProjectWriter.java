package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistryJson;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.io.SceneWriter;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.utils.AtomicFiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileProjectWriter implements ProjectWriter {

    private final SceneWriter sceneWriter;
    private final ObjectMapper mapper;

    @Override
    public Path createProject(Path dir, String name) throws IOException {
        Files.createDirectories(dir);

        Path scene = dir.resolve(name + ".gltf");
        ProjectAssets.requireInside(dir, scene);
        if (Files.exists(scene, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new java.nio.file.FileAlreadyExistsException(scene.toString());
        }
        new ProjectConfigStore(mapper).prepareNewScene(scene);
        Files.writeString(scene, ProjectTemplates.MINIMAL_GLTF, StandardOpenOption.CREATE_NEW);

        Path archetypes = ProjectTemplates.archetypesFileFor(scene);
        if (!Files.exists(archetypes)) {
            Files.writeString(archetypes, ProjectTemplates.EMPTY_ARCHETYPES, StandardOpenOption.CREATE_NEW);
        }

        return scene;
    }

    @Override
    public void saveScene(Path path, Scene sceneData) throws SceneFormatException, IOException {
        sceneWriter.write(sceneData, path);
    }

    @Override
    public void saveArchetypes(Path scene, ArchetypeRegistry registry) throws IOException {
        AtomicFiles.write(
            ProjectTemplates.archetypesFileFor(scene),
            mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(ArchetypeRegistryJson.from(registry))
        );
    }
}
