package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.ArchetypeCatalog;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeRegistry;
import org.halalfoursome.kebabeditor.scene.error.SceneException;
import org.halalfoursome.kebabeditor.scene.error.SceneFormatException;
import org.halalfoursome.kebabeditor.scene.model.Scene;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResolver;
import org.halalfoursome.kebabeditor.scene.resolve.PrefabResolver;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProjectManager {

    private final List<Runnable> listeners = new ArrayList<>();
    private final RecentProjects recentProjects;
    private final ProjectWriter writer;
    private final ProjectLoader loader;

    private byte[] savedSceneHash;

    private Maybe<ProjectRepository> repository = Maybe.none();

    public void create(Path dir, String name) throws SceneException, IOException {
        open(writer.createProject(dir, name));
    }

    public void open(Path file) throws SceneException, IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a file: " + file);
        }

        ProjectRepository loaded = loader.load(file);
        byte[] hash = sceneHash(file);
        repository = Maybe.some(loaded);
        savedSceneHash = hash;
        recentProjects.add(file);
        notifyListeners();
    }

    public void close() {
        repository = Maybe.none();
        notifyListeners();
    }

    public boolean isOpen() {
        return repository.isSome();
    }

    public Maybe<Path> currentFile() {
        return repository.map(ProjectRepository::filePath);
    }

    public List<ArchetypeDefinition> archetypes() {
        return repository
            .map(r -> r.archetypeCatalog()
                .all()
                .stream()
                .sorted(Comparator.comparing(ArchetypeDefinition::getId))
                .toList()
            )
            .unwrapOr(List.of());
    }

    public Maybe<Scene> scene() {
        return repository.map(ProjectRepository::sceneData);
    }

    public Maybe<ArchetypeCatalog> catalog() {
        return repository.map(ProjectRepository::archetypeCatalog);
    }

    public Maybe<ModelResolver> modelResolver() {
        return repository.map(ProjectRepository::modelResolver);
    }

    public Maybe<PrefabResolver> prefabResolver() {
        return repository.map(ProjectRepository::prefabResolver);
    }

    public boolean isBuiltin(String archetypeId) {
        return repository
            .map(r -> r.archetypeCatalog().isBuiltin(archetypeId))
            .unwrapOr(false);
    }

    public void addArchetype(ArchetypeDefinition definition) throws IOException {
        if (repository.isNone()) {
            throw new IllegalStateException("No project is open");
        }

        ProjectRepository current = repository.unwrap();
        ArchetypeRegistry registry = current.archetypeCatalog().getRegistry();

        if (current.archetypeCatalog().isBuiltin(definition.getId())) {
            throw new IllegalArgumentException(
                "Archetype '" + definition.getId() + "' is built in and cannot be redefined"
            );
        }

        registry.add(definition);

        try {
            writer.saveArchetypes(current.filePath(), registry);
        } catch (IOException e) {
            registry.remove(definition);
            throw e;
        }

        current.prefabResolver().invalidateAll();
        notifyListeners();
    }

    public void removeArchetype(ArchetypeDefinition definition) throws IOException {
        if (repository.isNone()) {
            throw new IllegalStateException("No project is open");
        }

        ProjectRepository current = repository.unwrap();
        ArchetypeRegistry registry = current.archetypeCatalog().getRegistry();

        if (current.archetypeCatalog().isBuiltin(definition.getId())) {
            throw new IllegalArgumentException(
                "Archetype '" + definition.getId() + "' is built in and cannot be removed"
            );
        }

        registry.remove(definition);

        try {
            writer.saveArchetypes(current.filePath(), registry);
        } catch (IOException e) {
            registry.add(definition);
            throw e;
        }

        current.prefabResolver().invalidateAll();
        notifyListeners();
    }

    public void saveArchetypes() throws IOException {
        if (repository.isNone()) {
            throw new IllegalStateException("No project is open");
        }

        ProjectRepository current = repository.unwrap();
        writer.saveArchetypes(current.filePath(), current.archetypeCatalog().getRegistry());
        current.prefabResolver().invalidateAll();
    }

    public void saveScene() throws SceneFormatException, IOException {
        ProjectRepository current = repository.expect("No project is open");
        if (!java.util.Arrays.equals(savedSceneHash, sceneHash(current.filePath()))) {
            throw new IOException("Scene changed on disk in another window or application; reopen it before saving: " + current.filePath());
        }
        writer.saveScene(current.filePath(), current.sceneData());
        savedSceneHash = sceneHash(current.filePath());
    }

    public void save() throws SceneFormatException, IOException {
        saveScene();
        ProjectRepository current = repository.unwrap();
        writer.saveArchetypes(current.filePath(), current.archetypeCatalog().getRegistry());
    }

    private static byte[] sceneHash(Path file) throws IOException {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public RecentProjects recentProjects() {
        return recentProjects;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public Maybe<ProjectAssets> assets() {
        return repository.map(ProjectRepository::assets);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
