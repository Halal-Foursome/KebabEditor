package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NoRecentProjects implements RecentProjects {

    private final List<Path> paths = new ArrayList<>();

    @Override
    public List<Path> paths() {
        return List.copyOf(paths);
    }

    @Override
    public void add(Path file) {
        paths.remove(file);
        paths.addFirst(file);
    }

    @Override
    public void remove(Path file) {
        paths.remove(file);
    }

    @Override
    public void clear() {
        paths.clear();
    }
}
