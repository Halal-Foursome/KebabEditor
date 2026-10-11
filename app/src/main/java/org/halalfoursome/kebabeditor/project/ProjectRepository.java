package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.scene.model.Scene;

public record ProjectRepository(
    Path filePath,
    Scene sceneData,
    ArchetypeCatalog archetypeCatalog
) {}
