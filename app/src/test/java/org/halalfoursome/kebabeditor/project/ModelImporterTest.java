package org.halalfoursome.kebabeditor.project;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class ModelImporterTest {

    @TempDir
    Path project;

    @TempDir
    Path source;

    private final ModelImporter importer = new ModelImporter(new ObjectMapper());

    @Test
    void modelInsideTheProjectIsUsedWhereItIs() throws Exception {
        Path inside = Files.createDirectories(project.resolve("art")).resolve("rock.glb");
        Files.write(inside, new byte[] { 1, 2, 3 });

        assertEquals(inside, importer.importInto(project, inside));
        assertFalse(Files.exists(project.resolve(ModelImporter.MODELS_DIR)));
    }

    @Test
    void glbIsCopiedWithoutOverwritingAnEarlierImport() throws Exception {
        Path glb = source.resolve("rock.glb");
        Files.write(glb, new byte[] { 1, 2, 3 });

        Path first = importer.importInto(project, glb);
        Path second = importer.importInto(project, glb);

        assertEquals(project.resolve("models/rock.glb"), first);
        assertEquals(project.resolve("models/rock_2.glb"), second);
        assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(second));
    }

    @Test
    void gltfComesWithTheFilesItReferences() throws Exception {
        Files.createDirectories(source.resolve("tex"));
        Files.write(source.resolve("crate data.bin"), new byte[] { 9 });
        Files.write(source.resolve("tex/wood.png"), new byte[] { 7 });
        Path gltf = source.resolve("crate.gltf");
        Files.writeString(gltf, """
            {
              "buffers": [ { "uri": "crate%20data.bin", "byteLength": 1 },
                           { "uri": "data:application/octet-stream;base64,AA==", "byteLength": 1 } ],
              "images": [ { "uri": "tex/wood.png" }, { "uri": "../outside.png" } ]
            }
            """);

        Path imported = importer.importInto(project, gltf);

        Path folder = project.resolve("models/crate");
        assertEquals(folder.resolve("crate.gltf"), imported);
        assertTrue(Files.isRegularFile(folder.resolve("crate data.bin")));
        assertTrue(Files.isRegularFile(folder.resolve("tex/wood.png")));
        // Files outside the model's folder are never pulled in.
        assertFalse(Files.exists(project.resolve("models/outside.png")));
    }
}
