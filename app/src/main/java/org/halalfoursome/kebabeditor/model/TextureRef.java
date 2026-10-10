package org.halalfoursome.kebabeditor.model;

import java.nio.file.Path;
import java.util.Objects;

public record TextureRef(Path path) {

    public TextureRef {
        Objects.requireNonNull(path, "path");
        path = path.toAbsolutePath().normalize();
    }
}
