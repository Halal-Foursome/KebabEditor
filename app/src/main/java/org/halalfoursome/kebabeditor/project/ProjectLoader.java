package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistryJson;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.io.GltfSceneReader;
import org.halalfoursome.kebabeditor.scene.model.Scene;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class ProjectLoader {

    private final GltfSceneReader sceneReader;
    private final ObjectMapper mapper;
    
    public ProjectRepository load(Path file) throws SceneException, IOException {
        JsonNode root = mapper.readTree(file.toFile());

        Path registryPath = ProjectTemplates.archetypesFileFor(file);
        ArchetypeRegistry registry = loadRegistry(registryPath);

        Scene scene = sceneReader.read(root, registry);

        return new ProjectRepository(file, scene, registry);
    }

    // A missing registry is just an empty one; 
    // the file is created on the first save
    private ArchetypeRegistry loadRegistry(Path registryPath) throws IOException {
        if (!Files.exists(registryPath)) {
            return new ArchetypeRegistry();
        }

        return mapper.readValue(
            Files.readString(registryPath),
            ArchetypeRegistryJson.class
        ).toRegistry();
    }
}
