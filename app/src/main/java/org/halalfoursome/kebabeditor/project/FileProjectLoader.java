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
import org.halalfoursome.kebabeditor.scene.resolve.CachingModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.CachingPrefabResolver;
import org.halalfoursome.kebabeditor.scene.resolve.ModelExtractor;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResolver;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

// A project keeps its scene-file entry point, extended by optional kebab-project.json scope.
@RequiredArgsConstructor
public class FileProjectLoader implements ProjectLoader {

    private final SceneReader sceneReader;
    private final ObjectMapper mapper;
    private final ModelExtractor modelExtractor;

    @Override
    public ProjectRepository load(Path file) throws SceneException, IOException {
        ProjectAssets assets = new ProjectConfigStore(mapper).loadForScene(file);
        Path registryPath = ProjectTemplates.archetypesFileFor(file);
        ArchetypeRegistry registry = loadRegistry(registryPath);
        ArchetypeCatalog catalog = new ArchetypeCatalog(registry);

        Scene scene = sceneReader.read(file, catalog);

        // Each project gets its own resolver, rooted at the project folder, so models never leak between projects
        Path projectRoot = assets.root();
        ModelResolver modelResolver = new CachingModelResolver(modelExtractor, projectRoot);

        PrefabResolver prefabResolver = new CachingPrefabResolver(sceneReader, catalog, projectRoot);

        return new ProjectRepository(file, scene, catalog, modelResolver, prefabResolver, assets);
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
