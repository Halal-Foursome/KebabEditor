package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProjectManager {

    private final List<Runnable> listeners = new ArrayList<>();
    private final RecentProjects recentProjects;
    private final ProjectWriter writer;
    private final ProjectLoader loader;

    private Maybe<ProjectRepository> repository = Maybe.none();

    public void create(Path dir, String name) throws IOException {
        open(writer.createProject(dir, name));
    }

    public void open(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a file: " + file);
        }

        repository = Maybe.some(loader.load(file));
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
            .map(r -> r.archetypeRegistry().getArchetypes().stream()
                .sorted(Comparator.comparing(ArchetypeDefinition::getId))
                .toList()
            )
            .unwrapOr(List.of());
    }

    public RecentProjects recentProjects() {
        return recentProjects;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
