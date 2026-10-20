package org.halalfoursome.kebabeditor.assets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/** The same classification drives both icons and opening. No extension-only scene heuristic. */
public final class AssetOpenHandler {
    private final Consumer<Path> models;
    private final Consumer<Path> scenes;
    public AssetOpenHandler(Consumer<Path> models, Consumer<Path> scenes) {
        this.models = models;
        this.scenes = scenes;
    }
    public void open(AssetService service, Path path) throws IOException {
        open(service.type(path), path);
    }
    public void open(AssetType type, Path path) {
        switch (type) {
            case MODEL -> models.accept(path);
            case SCENE -> scenes.accept(path);
            default -> { }
        }
    }
}
