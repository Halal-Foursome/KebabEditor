package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import org.halalfoursome.kebabeditor.logging.LogService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Brings a model file into the project, since models are only resolved inside
 * the project folder. A model already inside it is used where it is; anything
 * else is copied to {@code models/}. A .gltf goes into its own folder together
 * with the buffers and images it references, keeping their relative layout.
 */
@RequiredArgsConstructor
public class ModelImporter {

    public static final String MODELS_DIR = "models";

    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:");

    private final ObjectMapper mapper;

    /** @return the model's path inside the project */
    public Path importInto(Path projectRoot, Path model) throws IOException {
        Path root = projectRoot.toAbsolutePath().normalize();
        Path source = model.toAbsolutePath().normalize();

        if (source.startsWith(root)) {
            return source;
        }

        Path modelsDir = Files.createDirectories(root.resolve(MODELS_DIR));
        String fileName = source.getFileName().toString();

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".gltf")) {
            Path target = unique(modelsDir, fileName);
            Files.copy(source, target);
            LogService.get().info("Copied model " + fileName + " into " + root.relativize(target));
            return target;
        }

        Path folder = Files.createDirectories(unique(modelsDir, stem(fileName)));
        Path target = folder.resolve(fileName);
        Files.copy(source, target);
        copyDependencies(source, folder);

        LogService.get().info("Copied model " + fileName + " and its files into " + root.relativize(folder));
        return target;
    }

    private void copyDependencies(Path gltf, Path folder) throws IOException {
        JsonNode doc = mapper.readTree(gltf.toFile());
        Path sourceDir = gltf.getParent();

        for (String section : new String[] { "buffers", "images" }) {
            for (JsonNode entry : doc.path(section)) {
                String uri = entry.path("uri").asText("");
                if (uri.isEmpty() || SCHEME.matcher(uri).find()) {
                    continue; // embedded data: URI or remote resource
                }

                String relative = URLDecoder.decode(uri.replace("+", "%2B"), StandardCharsets.UTF_8);
                Path from = sourceDir.resolve(relative).normalize();
                Path to = folder.resolve(relative).normalize();

                if (!from.startsWith(sourceDir) || !to.startsWith(folder)) {
                    LogService.get().warn("Not copying '" + uri + "': it lies outside the model's folder");
                    continue;
                }
                if (!Files.isRegularFile(from)) {
                    LogService.get().warn("Model file '" + uri + "' referenced by " + gltf.getFileName() + " is missing");
                    continue;
                }

                Files.createDirectories(to.getParent());
                if (!Files.exists(to)) {
                    Files.copy(from, to);
                }
            }
        }
    }

    // name, name_2, name_3, ... so an import never overwrites an existing asset
    private static Path unique(Path dir, String name) {
        Path candidate = dir.resolve(name);
        String stem = stem(name);
        String extension = name.substring(stem.length());

        for (int n = 2; Files.exists(candidate); n++) {
            candidate = dir.resolve(stem + "_" + n + extension);
        }
        return candidate;
    }

    private static String stem(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
