package org.halalfoursome.kebabeditor.assets;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.halalfoursome.kebabeditor.scene.resolve.GltfMeshExtractor;

public final class AssetService {

    public record Entry(Path path, AssetType type) {

        public boolean directory() {
            return type == AssetType.FOLDER;
        }

        public String name() {
            return path.getFileName().toString();
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private static final Comparator<Entry> ORDER = Comparator.comparing(Entry::directory)
        .reversed()
        .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Entry::name);

    private final ProjectAssets project;
    private final Predicate<Path> isModel;

    public AssetService(ProjectAssets project) {
        this(project, new GltfMeshExtractor()::supports);
    }

    public AssetService(ProjectAssets project, Predicate<Path> isModel) {
        this.project = project;
        this.isModel = isModel;
    }

    public ProjectAssets project() {
        return project;
    }

    public List<Entry> list(Path directory) throws IOException {
        if (!project.visible(directory, true)) {
            throw new IOException("Directory outside configured asset scope: " + directory);
        }
        List<Entry> entries = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path path : children) {
                // Symlinks are skipped, which also rules out recursive aliases
                if (Files.isSymbolicLink(path)) {
                    continue;
                }
                boolean folder = Files.isDirectory(path);
                if (project.visible(path, folder)) {
                    entries.add(new Entry(path, AssetType.of(path, folder, project, isModel)));
                }
            }
        }
        entries.sort(ORDER);
        return List.copyOf(entries);
    }

    public AssetType type(Path file) throws IOException {
        if (!Files.isRegularFile(file) || !project.visible(file, false)) {
            throw new IOException("Asset is missing or outside configured scope: " + file);
        }
        return AssetType.of(file, false, project, isModel);
    }
}
