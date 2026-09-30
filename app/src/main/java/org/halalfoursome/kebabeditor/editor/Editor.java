package org.halalfoursome.kebabeditor.editor;

import java.io.IOException;
import java.nio.file.Path;

import org.halalfoursome.kebabeditor.project.ProjectManager;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Editor {

    private final ProjectManager projectManager;

    public void createProject(Path dir, String name) throws IOException {
        projectManager.create(dir, name);
    }

    public void openProject(Path file) {
        projectManager.open(file);
    }

    public boolean isProjectOpen() {
        return projectManager.isOpen();
    }

    public void addProjectListener(Runnable listener) {
        projectManager.addListener(listener);
    }
}
