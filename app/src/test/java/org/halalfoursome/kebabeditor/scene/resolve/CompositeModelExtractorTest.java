package org.halalfoursome.kebabeditor.scene.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.junit.jupiter.api.Test;

class CompositeModelExtractorTest {

    // A made-up format, standing in for something like OBJ added later
    private static final class FakeObjExtractor implements ModelExtractor {

        @Override
        public boolean supports(Path file) {
            return file.getFileName().toString().endsWith(".obj");
        }

        @Override
        public List<ModelPart> extract(Path file, Path assetRoot) {
            return List.of(new ModelPart(Meshes.CUBE_TRIANGLES, Material.DEFAULT, Mat4.identity()));
        }
    }

    private final CompositeModelExtractor composite = new CompositeModelExtractor(
        List.of(new GltfMeshExtractor(), new FakeObjExtractor())
    );

    @Test
    void gltfExtractorClaimsGltfAndGlbCaseInsensitively() {
        GltfMeshExtractor gltf = new GltfMeshExtractor();

        assertTrue(gltf.supports(Path.of("a.gltf")));
        assertTrue(gltf.supports(Path.of("dir/A.GLB")));
        assertFalse(gltf.supports(Path.of("a.obj")));
        assertFalse(gltf.supports(Path.of("gltf")));
    }

    @Test
    void supportsWhatAnyMemberSupports() {
        assertTrue(composite.supports(Path.of("a.gltf")));
        assertTrue(composite.supports(Path.of("a.obj")));
        assertFalse(composite.supports(Path.of("a.fbx")));
    }

    @Test
    void dispatchesToTheExtractorThatSupportsTheFile() throws Exception {
        List<ModelPart> parts = composite.extract(Path.of("model.obj"), Path.of("."));

        assertEquals(1, parts.size());
        assertEquals(Meshes.CUBE_TRIANGLES, parts.get(0).mesh());
    }

    @Test
    void unsupportedFormatIsReported() {
        ModelLoadException error = assertThrows(
            ModelLoadException.class,
            () -> composite.extract(Path.of("model.fbx"), Path.of("."))
        );

        assertTrue(error.getMessage().contains("model.fbx"), error.getMessage());
    }
}
