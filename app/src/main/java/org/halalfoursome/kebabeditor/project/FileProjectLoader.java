package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistryJson;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.io.SceneReader;
import org.halalfoursome.kebabeditor.scene.model.Scene;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

// A project is a scene file with an archetypes.json next to it
@RequiredArgsConstructor
public class FileProjectLoader implements ProjectLoader {

    private final SceneReader sceneReader;
    private final ObjectMapper mapper;

    @Override
    public ProjectRepository load(Path file) throws SceneException, IOException {
        Path registryPath = ProjectTemplates.archetypesFileFor(file);
        ArchetypeRegistry registry = loadRegistry(registryPath);
        ArchetypeCatalog catalog = new ArchetypeCatalog(registry);

        Scene scene = sceneReader.read(file, catalog);

        return new ProjectRepository(file, scene, catalog);
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
