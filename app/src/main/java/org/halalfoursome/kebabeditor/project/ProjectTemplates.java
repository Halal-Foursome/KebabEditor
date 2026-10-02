package org.halalfoursome.kebabeditor.project;

import java.nio.file.Path;

public class ProjectTemplates {
    
    public static final String ARCHETYPES_FILE = "archetypes.json";

    // One archetypes file is shared by all scenes in the same folder
    public static Path archetypesFileFor(Path scene) {
        return scene.toAbsolutePath().resolveSibling(ARCHETYPES_FILE);
    }

    public static final String MINIMAL_GLTF = """
        {
          "asset": {
            "version": "2.0"
          }
        }
        """;

    public static final String EMPTY_ARCHETYPES = """
        {
          "archetypes": []
        }
        """;
}
