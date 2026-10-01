package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;

import com.fasterxml.jackson.databind.ObjectMapper;

public record ProjectRepository(
    Path filePath,
    // Temporary scene repo
    String currentSceneData,
    ArchetypeRegistry archetypeRegistry
) {

    public static ProjectRepository load(Path file) throws IOException {
        // Temporarily, scene is just an unparsed glTF string 🥂
        String currentSceneData = Files.readString(file);

        Path registryPath = file.toAbsolutePath()
            .resolveSibling(ProjectManager.ARCHETYPES_FILE);

        if (!Files.exists(registryPath)) {
            Files.writeString(registryPath, ProjectManager.EMPTY_ARCHETYPES);
        }

        String registryJson = Files.readString(registryPath);
        ObjectMapper mapper = new ObjectMapper();
        ArchetypeRegistry registry = mapper.readValue(
            registryJson, 
            ArchetypeRegistry.class
        );

        return new ProjectRepository(file, currentSceneData, registry);
    }
}
