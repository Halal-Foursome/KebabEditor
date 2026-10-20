package org.halalfoursome.kebabeditor.assets;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import org.halalfoursome.kebabeditor.project.ProjectAssets;
import org.halalfoursome.kebabeditor.utils.LucideIcon;

public enum AssetType {
    FOLDER(LucideIcon.FOLDER),
    SCENE(LucideIcon.COMPONENT),
    MODEL(LucideIcon.BOXES),
    IMAGE(LucideIcon.IMAGE),
    JSON(LucideIcon.PARENTHESES),
    FILE(LucideIcon.FILE);

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    public final LucideIcon icon;

    AssetType(LucideIcon icon) {
        this.icon = icon;
    }

    public static AssetType of(Path path, boolean directory, ProjectAssets project, Predicate<Path> isModel) {
        if (directory) {
            return FOLDER;
        }
        if (project.isScene(path)) {
            return SCENE;
        }
        if (isModel.test(path)) {
            return MODEL;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return IMAGE;
        }
        return extension.equals("json") ? JSON : FILE;
    }
}
