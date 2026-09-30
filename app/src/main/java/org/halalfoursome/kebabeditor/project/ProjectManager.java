package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectManager {

    private static final String MINIMAL_GLTF = """
        {
          "asset": {
            "version": "2.0"
          }
        }
        """;

    private static final String EMPTY_ARCHETYPES = """
        {
          "archetypes": []
        }
        """;

    public static final String ARCHETYPES_FILE = "archetypes.json";

    private Path current;
    private final List<Runnable> listeners = new ArrayList<>();

    public void create(Path dir, String name) throws IOException {
        Files.createDirectories(dir);

        Path scene = dir.resolve(name + ".gltf");
        Files.writeString(scene, MINIMAL_GLTF, StandardOpenOption.CREATE_NEW);

        // one archetypes file is shared by all scenes in the same folder
        Path archetypes = dir.resolve(ARCHETYPES_FILE);
        if (!Files.exists(archetypes)) {
            Files.writeString(archetypes, EMPTY_ARCHETYPES);
        }

        open(scene);
    }

    public void open(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Not a file: " + file);
        }

        current = file;
        notifyListeners();
    }

    public void close() {
        current = null;
        notifyListeners();
    }

    public boolean isOpen() {
        return current != null;
    }

    public Optional<Path> currentFile() {
        return Optional.ofNullable(current);
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
