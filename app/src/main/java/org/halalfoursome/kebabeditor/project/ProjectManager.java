package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.halalfoursome.kebabeditor.utils.Maybe;

public class ProjectManager {

    public static final String ARCHETYPES_FILE = "archetypes.json";

    private final List<Runnable> listeners = new ArrayList<>();
    private Maybe<ProjectRepository> repository = Maybe.none();

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

    public void open(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a file: " + file);
        }

        repository = Maybe.some(ProjectRepository.load(file));
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

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }

    public static final String MINIMAL_GLTF = """
        {
          "asset": {
            "version": "2.0"
          }
        }
        """;

    public static final String EMPTY_ARCHETYPES = """
        {
          "archetypes": []
        }
        """;
}
