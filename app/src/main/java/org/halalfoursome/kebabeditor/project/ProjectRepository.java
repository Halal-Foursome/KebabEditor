package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;

public record ProjectRepository(
    Path filePath,
    // Temporary scene repo
    String currentSceneData,
    ArchetypeRegistry archetypeRegistry
) {}
