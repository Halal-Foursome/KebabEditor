package org.halalfoursome.kebabeditor.assets;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.halalfoursome.kebabeditor.project.ProjectAssets;

public final class AssetService {
    public record Entry(Path path, AssetType type) {
        public boolean directory() { return type == AssetType.FOLDER; }
        @Override public String toString() { return path.getFileName().toString(); }
    }
    private final ProjectAssets project;
    public AssetService(ProjectAssets project) { this.project = project; }
    public ProjectAssets project() { return project; }

    public List<Entry> list(Path directory) throws IOException {
        if (!project.visible(directory, true)) throw new IOException("Directory outside configured asset scope: " + directory);
        List<Entry> entries = new ArrayList<>();
        try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
            for (Path path : children) {
                // Do not follow directory symlinks: this also prevents recursive aliases and cycles.
                if (Files.isSymbolicLink(path)) continue;
                boolean folder = Files.isDirectory(path);
                if (project.visible(path, folder)) entries.add(new Entry(path, AssetType.of(path, folder, project)));
            }
        }
        entries.sort(Comparator.comparing(Entry::directory).reversed()
            .thenComparing(e -> e.path().getFileName().toString(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(e -> e.path().getFileName().toString()));
        return List.copyOf(entries);
    }

    public AssetType type(Path file) throws IOException {
        if (!project.visible(file, Files.isDirectory(file)) || !Files.isRegularFile(file)) {
            throw new IOException("Asset is missing or outside configured scope: " + file);
        }
        return AssetType.of(file, false, project);
    }
}
