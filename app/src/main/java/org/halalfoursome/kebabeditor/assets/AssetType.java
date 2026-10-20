package org.halalfoursome.kebabeditor.assets;

import java.nio.file.Path;
import java.util.Locale;
import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.halalfoursome.kebabeditor.utils.LucideIcon;

public enum AssetType {
    FOLDER(LucideIcon.FOLDER), SCENE(LucideIcon.COMPONENT), MODEL(LucideIcon.BOXES),
    IMAGE(LucideIcon.IMAGE), JSON(LucideIcon.PARENTHESES), FILE(LucideIcon.FILE);

    public final LucideIcon icon;
    AssetType(LucideIcon icon) { this.icon = icon; }

    public static AssetType of(Path path, boolean directory, ProjectAssets project) {
        if (directory) return FOLDER;
        if (project.isScene(path)) return SCENE;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".gltf") || name.endsWith(".glb")) return MODEL;
        if (name.matches(".*\\.(png|jpe?g|gif|bmp|webp)$")) return IMAGE;
        return name.endsWith(".json") ? JSON : FILE;
    }
}
