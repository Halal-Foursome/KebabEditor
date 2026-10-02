package org.halalfoursome.kebabeditor.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.archetype.definition.ArchetypeDefinition;
import org.halalfoursome.kebabeditor.project.ProjectManager;
import org.halalfoursome.kebabeditor.project.RecentProjects;
import org.halalfoursome.kebabeditor.utils.Maybe;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Editor {

    private final ProjectManager projectManager;

    public void createProject(Path dir, String name) throws IOException {
        projectManager.create(dir, name);
    }

    public void openProject(Path file) throws IOException {
        projectManager.open(file);
    }

    public void saveProject() throws IOException {
        projectManager.save();
    }

    public Maybe<Path> currentProjectFile() {
        return projectManager.currentFile();
    }

    public boolean isProjectOpen() {
        return projectManager.isOpen();
    }

    public List<ArchetypeDefinition> archetypes() {
        return projectManager.archetypes();
    }

    public void addArchetype(ArchetypeDefinition definition) throws IOException {
        projectManager.addArchetype(definition);
    }

    public void saveArchetypes() throws IOException {
        projectManager.saveArchetypes();
    }

    public RecentProjects recentProjects() {
        return projectManager.recentProjects();
    }

    public void addProjectListener(Runnable listener) {
        projectManager.addListener(listener);
    }
}
