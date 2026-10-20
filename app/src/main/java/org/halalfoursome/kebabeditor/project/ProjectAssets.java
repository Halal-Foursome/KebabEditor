package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/** Immutable project scope shared by browsers, never mutable scene state. */
public record ProjectAssets(Path root, List<Path> folders, List<Path> scenes) {
    public ProjectAssets {
        root = root.toAbsolutePath().normalize();
        folders = List.copyOf(folders);
        scenes = List.copyOf(scenes);
    }

    /** Checks existing ancestors too, so missing children of an escaping symlink are rejected. */
    public static Path resolveInside(Path root, String relative) throws IOException {
        try {
            Path path = Path.of(relative);
            if (relative.isBlank() || path.isAbsolute() || relative.contains("\\")
                || relative.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
                throw new IOException("Expected a relative project path: " + relative);
            }
            Path resolved = root.resolve(path).toAbsolutePath().normalize();
            requireInside(root, resolved);
            return resolved;
        } catch (InvalidPathException e) {
            throw new IOException("Invalid project path: " + relative, e);
        }
    }

    public static void requireInside(Path root, Path path) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedRoot)) throw new IOException("Path outside project: " + path);
        Path ancestor = normalized;
        while (!Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) ancestor = ancestor.getParent();
        if (!ancestor.toRealPath().resolve(ancestor.relativize(normalized)).normalize().startsWith(realOrMissing(normalizedRoot))) {
            throw new IOException("Symbolic link outside project: " + path);
        }
    }

    private static Path realOrMissing(Path path) throws IOException {
        Path ancestor = path;
        while (!Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) ancestor = ancestor.getParent();
        return ancestor.toRealPath().resolve(ancestor.relativize(path)).normalize();
    }

    public boolean isScene(Path path) {
        return scenes.contains(path.toAbsolutePath().normalize());
    }

    public boolean visible(Path path, boolean directory) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        requireInside(root, normalized);
        if (normalized.equals(root)) return true;
        if (!directory && normalized.getParent().equals(root)) return true;
        return folders.stream().anyMatch(folder -> normalized.startsWith(folder)
                || (directory && folder.startsWith(normalized)))
            || scenes.stream().anyMatch(scene -> scene.equals(normalized)
                || (directory && scene.startsWith(normalized)));
    }
}
