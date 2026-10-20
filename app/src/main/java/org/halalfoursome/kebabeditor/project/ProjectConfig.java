package org.halalfoursome.kebabeditor.project;

import java.util.List;

/** Paths are relative to assetRoot; assetRoot is relative to the configuration directory. */
public record ProjectConfig(int version, String assetRoot, List<String> assetFolders, List<String> scenes) {
    public ProjectConfig {
        assetFolders = List.copyOf(assetFolders);
        scenes = List.copyOf(scenes);
    }
}
