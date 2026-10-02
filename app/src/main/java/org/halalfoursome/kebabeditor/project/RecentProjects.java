package org.halalfoursome.kebabeditor.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecentProjects {

    public static final String CONFIG_FOLDER = "kebabeditor";
    public static final String RECENT_FILE = "recent.txt";
    private static final int MAX = 10;

    private final Path configPath = configPath();
    private final List<Path> paths = new ArrayList<>();

    public RecentProjects() {
        load();
    }

    public List<Path> paths() {
        return List.copyOf(paths);
    }

    public void add(Path file) {
        Path path = file.toAbsolutePath().normalize();
        paths.remove(path);
        paths.add(0, path);

        if (paths.size() > MAX) {
            paths.subList(MAX, paths.size()).clear();
        }

        save();
    }

    public void remove(Path file) {
        paths.remove(file.toAbsolutePath().normalize());
        save();
    }

    public void clear() {
        paths.clear();
        save();
    }

    private void load() {
        if (!Files.isRegularFile(configPath)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(configPath)) {
                if (!line.isBlank() && Files.isRegularFile(Path.of(line.trim()))) {
                    paths.add(Path.of(line.trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot read " + configPath + ": " + e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.write(configPath, paths.stream().map(Path::toString).toList());
        } catch (IOException e) {
            System.err.println("Cannot save " + configPath + ": " + e);
        }
    }

    private static Path configPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, CONFIG_FOLDER, RECENT_FILE);
        }
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", CONFIG_FOLDER, RECENT_FILE);
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, CONFIG_FOLDER, RECENT_FILE);
        }
        
        return Path.of(home, ".config", CONFIG_FOLDER, RECENT_FILE);
    }
}
