package org.halalfoursome.kebabeditor.editor;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.project.ProjectManager;
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

    public Maybe<Path> currentProjectFile() {
        return projectManager.currentFile();
    }

    public boolean isProjectOpen() {
        return projectManager.isOpen();
    }

    public void addProjectListener(Runnable listener) {
        projectManager.addListener(listener);
    }
}
