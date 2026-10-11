package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;

public record ProjectRepository(
    Path filePath,
    Scene sceneData,
    ArchetypeCatalog archetypeCatalog,
    ModelResolver modelResolver
) {}
