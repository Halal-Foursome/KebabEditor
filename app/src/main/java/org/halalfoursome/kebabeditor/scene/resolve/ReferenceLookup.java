package org.halalfoursome.kebabeditor.scene.resolve;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.ReferenceLookup.*;
import org.halalfoursome.kebabeditor.utils.AssetPaths;

public sealed interface ReferenceLookup
    permits
        Found,
        Failed
{
    record Found(Path file) implements ReferenceLookup {

        public Found {
            Objects.requireNonNull(file, "file");
        }
    }

    record Failed(Path path, Reason reason, String detail) implements ReferenceLookup {

        public Failed {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(detail, "detail");
        }
    }

    static ReferenceLookup locate(Path scene, String relPath, Path assetRoot, String kind) {
        Path directory = scene.toAbsolutePath().normalize().getParent();

        if (relPath.isBlank()) {
            return new Failed(directory, Reason.MISSING, "No " + kind + " path is set");
        }

        Path file = AssetPaths.resolveInside(directory, assetRoot, relPath).unwrapOrNull();

        if (file == null) {
            return new Failed(
                directory.resolve(relPath).normalize(),
                Reason.UNREADABLE,
                "'" + relPath + "' is outside the project or not a file path"
            );
        }

        if (!Files.isRegularFile(file)) {
            return new Failed(file, Reason.MISSING, "File not found: " + file);
        }

        return new Found(file);
    }
}
