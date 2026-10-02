package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor 
public class ProjectLoader {

    private final ObjectMapper mapper;
    
    public ProjectRepository load(Path file) throws IOException {
        // Temporarily, scene is just an unparsed glTF string 🥂
        String currentSceneData = Files.readString(file);

        Path registryPath = file.toAbsolutePath()
            .resolveSibling(ProjectTemplates.ARCHETYPES_FILE);

        return new ProjectRepository(
            file, 
            currentSceneData, 
            loadRegistry(registryPath)
        );
    }

    // A missing registry is just an empty one; 
    // the file is created on the first save
    private ArchetypeRegistry loadRegistry(Path registryPath) throws IOException {
        if (!Files.exists(registryPath)) {
            return new ArchetypeRegistry();
        }

        return mapper.readValue(
            Files.readString(registryPath),
            ArchetypeRegistry.class
        );
    }
}
