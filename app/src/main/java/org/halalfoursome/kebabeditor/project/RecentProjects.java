package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;
import java.util.List;

public interface RecentProjects {

    List<Path> paths();

    void add(Path file);

    void remove(Path file);

    void clear();
}
