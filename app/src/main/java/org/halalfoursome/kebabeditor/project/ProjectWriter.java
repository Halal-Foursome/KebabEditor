package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistryJson;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneWriter;
import org.halalfoursome.kebabeditor.scene.model.Scene;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProjectWriter {

    private final GltfSceneWriter writer;
    private final ObjectMapper mapper;

    public Path createProject(Path dir, String name) throws IOException {
        Files.createDirectories(dir);

        Path scene = dir.resolve(name + ".gltf");
        Files.writeString(scene, ProjectTemplates.MINIMAL_GLTF, StandardOpenOption.CREATE_NEW);

        Path archetypes = ProjectTemplates.archetypesFileFor(scene);
        if (!Files.exists(archetypes)) {
            Files.writeString(archetypes, ProjectTemplates.EMPTY_ARCHETYPES);
        }

        return scene;
    }

    public void saveScene(Path path, Scene sceneData) throws SceneFormatException, IOException {
        JsonNode root = writer.write(sceneData);
        mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
    }

    public void saveArchetypes(Path scene, ArchetypeRegistry registry) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            ProjectTemplates.archetypesFileFor(scene).toFile(),
            ArchetypeRegistryJson.from(registry)
        );
    }
}
