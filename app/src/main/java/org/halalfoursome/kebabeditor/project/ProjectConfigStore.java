package org.halalfoursome.kebabeditor.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Configuration persistence and validation, independent of Swing. */
public final class ProjectConfigStore {
    public static final String FILE_NAME = "kebab-project.json";
    private final ObjectMapper mapper;

    public ProjectConfigStore(ObjectMapper mapper) { this.mapper = mapper; }

    public ProjectAssets loadForScene(Path scene) throws IOException {
        Path file = scene.toAbsolutePath().normalize();
        for (Path dir = file.getParent(); dir != null; dir = dir.getParent()) {
            Path config = dir.resolve(FILE_NAME);
            if (Files.exists(config, LinkOption.NOFOLLOW_LINKS)) return resolve(dir, read(config));
        }
        // Legacy imports used models/ as well as assets/: keep the whole scene folder accessible.
        return new ProjectAssets(file.getParent(), List.of(file.getParent()), List.of(file));
    }

    public ProjectConfig read(Path file) throws IOException {
        ProjectAssets.requireInside(file.toAbsolutePath().getParent(), file);
        JsonNode node = mapper.readTree(file.toFile());
        if (node == null || !node.isObject() || !node.path("version").isIntegralNumber()
            || !node.path("version").canConvertToInt() || node.path("version").intValue() != 1) {
            throw new IOException("Unsupported or missing project configuration version (expected 1): " + file);
        }
        if (!node.path("assetRoot").isTextual()) throw new IOException("assetRoot must be a string");
        ProjectConfig config = new ProjectConfig(1, node.get("assetRoot").textValue(),
            strings(node, "assetFolders"), strings(node, "scenes"));
        resolve(file.toAbsolutePath().getParent(), config);
        return config;
    }

    private static List<String> strings(JsonNode node, String field) throws IOException {
        if (!node.path(field).isArray()) throw new IOException(field + " must be an array of paths");
        List<String> result = new ArrayList<>();
        for (JsonNode value : node.get(field)) {
            if (!value.isTextual()) throw new IOException(field + " must contain only strings");
            result.add(value.textValue());
        }
        return result;
    }

    public ProjectAssets resolve(Path directory, ProjectConfig config) throws IOException {
        if (config.version() != 1) throw new IOException("Unsupported project configuration version: " + config.version());
        Path root = ProjectAssets.resolveInside(directory.toAbsolutePath().normalize(), config.assetRoot());
        if (Files.exists(root) && !Files.isDirectory(root)) throw new IOException("assetRoot is not a directory: " + root);
        List<Path> folders = new ArrayList<>();
        List<Path> scenes = new ArrayList<>();
        for (String folder : config.assetFolders()) {
            Path path = ProjectAssets.resolveInside(root, folder);
            if (Files.exists(path) && !Files.isDirectory(path)) throw new IOException("Asset folder is not a directory: " + path);
            folders.add(path);
        }
        for (String scene : config.scenes()) scenes.add(ProjectAssets.resolveInside(root, scene));
        return new ProjectAssets(root, folders, scenes);
    }

    /** Creation never replaces an existing config. Existing configs must explicitly register new scenes. */
    public void prepareNewScene(Path scene) throws IOException {
        Path dir = scene.toAbsolutePath().normalize().getParent();
        Path configFile = dir.resolve(FILE_NAME);
        ProjectAssets.resolveInside(dir, "assets");
        if (Files.exists(dir.resolve("assets")) && !Files.isDirectory(dir.resolve("assets"))) {
            throw new IOException("assets is not a directory");
        }
        if (Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
            ProjectAssets assets = resolve(dir, read(configFile));
            if (!assets.isScene(scene)) throw new IOException("Register " + scene.getFileName()
                + " in the existing " + FILE_NAME + " before creating it");
        } else {
            ProjectConfig config = new ProjectConfig(1, ".", List.of("assets", ModelImporter.MODELS_DIR), List.of(scene.getFileName().toString()));
            ProjectAssets.resolveInside(dir, "assets");
            Files.write(configFile, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config), StandardOpenOption.CREATE_NEW);
        }
        ProjectAssets.resolveInside(dir, "assets");
        Files.createDirectories(dir.resolve("assets"));
    }
}
