package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public record ProjectAssets(Path root, List<Path> folders, List<Path> scenes) {

    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.*");

    public ProjectAssets {
        root = root.toAbsolutePath().normalize();
        folders = List.copyOf(folders);
        scenes = List.copyOf(scenes);
    }

    // Existing ancestors are resolved too, so a missing child of an escaping symlink is rejected
    public static Path resolveInside(Path root, String relative) throws IOException {
        try {
            Path path = Path.of(relative);
            boolean notRelative = relative.isBlank()
                || path.isAbsolute()
                || relative.contains("\\")
                || SCHEME.matcher(relative).matches();
            if (notRelative) {
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
        if (!normalized.startsWith(normalizedRoot)) {
            throw new IOException("Path outside project: " + path);
        }
        if (!realOrMissing(normalized).startsWith(realOrMissing(normalizedRoot))) {
            throw new IOException("Symbolic link outside project: " + path);
        }
    }

    private static Path realOrMissing(Path path) throws IOException {
        Path ancestor = path;
        while (ancestor != null && !Files.exists(ancestor, LinkOption.NOFOLLOW_LINKS)) {
            ancestor = ancestor.getParent();
        }
        if (ancestor == null) {
            return path;
        }
        return ancestor.toRealPath().resolve(ancestor.relativize(path)).normalize();
    }

    public boolean isScene(Path path) {
        return scenes.contains(path.toAbsolutePath().normalize());
    }

    public boolean visible(Path path, boolean directory) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        requireInside(root, normalized);
        if (normalized.equals(root)) {
            return true;
        }
        if (!directory && root.equals(normalized.getParent())) {
            return true;
        }
        return folders.stream().anyMatch(folder -> covers(folder, normalized, directory))
            || scenes.stream().anyMatch(scene -> covers(scene, normalized, directory));
    }

    private static boolean covers(Path scope, Path path, boolean directory) {
        return path.startsWith(scope) || (directory && scope.startsWith(path));
    }
}
