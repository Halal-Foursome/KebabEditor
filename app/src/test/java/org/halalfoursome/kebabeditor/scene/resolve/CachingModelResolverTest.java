package org.halalfoursome.kebabeditor.scene.resolve;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.halalfoursome.kebabeditor.math.Aabb;
import org.halalfoursome.kebabeditor.math.Mat4;
import org.halalfoursome.kebabeditor.math.Vec3;
import org.halalfoursome.kebabeditor.model.Material;
import org.halalfoursome.kebabeditor.model.Meshes;
import org.halalfoursome.kebabeditor.model.ModelPart;
import org.halalfoursome.kebabeditor.scene.error.ModelLoadException;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.BrokenReference.Reason;
import org.halalfoursome.kebabeditor.scene.resolve.ModelResult.ResolvedModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CachingModelResolverTest {

    private static final float EPS = 1e-4f;

    private interface Script {
        List<ModelPart> run(Path file) throws ModelLoadException;
    }

    // Stands in for a real format; the script may call back into the resolver for nested references
    private static final class ScriptedExtractor implements ModelExtractor {

        int calls;
        Script script = file -> List.of(new ModelPart(Meshes.CUBE_TRIANGLES, Material.DEFAULT, Mat4.identity()));

        @Override
        public boolean supports(Path file) {
            return true;
        }

        @Override
        public List<ModelPart> extract(Path file, Path assetRoot) throws ModelLoadException {
            calls++;

            return script.run(file);
        }
    }

    @TempDir
    Path root;

    private final ScriptedExtractor extractor = new ScriptedExtractor();

    private CachingModelResolver resolver() {
        return new CachingModelResolver(extractor, root);
    }

    private Path scene() throws IOException {
        return touch("level.gltf");
    }

    private Path touch(String relative) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] { 1 });

        return file;
    }

    @Test
    void resolvesRelativeToTheSceneFolderAndComputesBounds() throws Exception {
        Path model = touch("models/crate.bin");
        extractor.script = file -> List.of(
            new ModelPart(Meshes.CUBE_TRIANGLES, Material.DEFAULT, Mat4.translation(new Vec3(10, 0, 0)))
        );

        ModelResult result = resolver().resolve(scene(), "models/crate.bin");

        ResolvedModel resolved = assertInstanceOf(ResolvedModel.class, result);
        assertEquals(model, resolved.path());
        assertEquals(1, resolved.parts().size());
        Aabb bounds = resolved.bounds();
        assertEquals(9.5f, bounds.getMinX(), EPS);
        assertEquals(10.5f, bounds.getMaxX(), EPS);
        assertEquals(-0.5f, bounds.getMinY(), EPS);
    }

    @Test
    void boundsAreTheUnionOfAllParts() throws Exception {
        touch("m.bin");
        extractor.script = file -> List.of(
            new ModelPart(Meshes.CUBE_TRIANGLES, Material.DEFAULT, Mat4.translation(new Vec3(-5, 0, 0))),
            new ModelPart(Meshes.CUBE_TRIANGLES, Material.DEFAULT, Mat4.translation(new Vec3(5, 0, 0)))
        );

        ResolvedModel resolved = assertInstanceOf(ResolvedModel.class, resolver().resolve(scene(), "m.bin"));

        assertEquals(-5.5f, resolved.bounds().getMinX(), EPS);
        assertEquals(5.5f, resolved.bounds().getMaxX(), EPS);
    }

    @Test
    void secondResolveComesFromTheCache() throws Exception {
        touch("m.bin");
        CachingModelResolver resolver = resolver();

        ModelResult first = resolver.resolve(scene(), "m.bin");
        ModelResult second = resolver.resolve(scene(), "m.bin");

        assertSame(first, second);
        assertEquals(1, extractor.calls);
    }

    @Test
    void changedFileIsReloaded() throws Exception {
        Path model = touch("m.bin");
        CachingModelResolver resolver = resolver();

        resolver.resolve(scene(), "m.bin");
        Files.setLastModifiedTime(model, FileTime.from(Instant.now().plusSeconds(60)));
        resolver.resolve(scene(), "m.bin");

        assertEquals(2, extractor.calls);
    }

    @Test
    void missingFileIsReportedWithItsPath() throws Exception {
        BrokenReference broken = assertInstanceOf(
            BrokenReference.class,
            resolver().resolve(scene(), "nope.bin")
        );

        assertEquals(Reason.MISSING, broken.reason());
        assertEquals(root.resolve("nope.bin"), broken.path());
        assertEquals(0, extractor.calls);
    }

    @Test
    void pathsLeavingTheProjectAreRefusedWithoutLoading() throws Exception {
        Path outside = root.getParent().resolve("outside-" + System.nanoTime() + ".bin");
        Files.write(outside, new byte[] { 1 });

        try {
            BrokenReference broken = assertInstanceOf(
                BrokenReference.class,
                resolver().resolve(scene(), "../" + outside.getFileName())
            );

            assertEquals(Reason.UNREADABLE, broken.reason());
            assertEquals(0, extractor.calls);
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void climbingInsideTheProjectIsAllowed() throws Exception {
        touch("shared/m.bin");
        Path nestedScene = touch("levels/one/level.gltf");

        assertInstanceOf(ResolvedModel.class, resolver().resolve(nestedScene, "../../shared/m.bin"));
    }

    @Test
    void extractorFailureBecomesUnreadableAndIsNotCached() throws Exception {
        touch("m.bin");
        extractor.script = file -> {
            throw new ModelLoadException("bad data");
        };
        CachingModelResolver resolver = resolver();

        BrokenReference broken = assertInstanceOf(BrokenReference.class, resolver.resolve(scene(), "m.bin"));
        assertEquals(Reason.UNREADABLE, broken.reason());
        assertEquals("bad data", broken.detail());

        extractor.script = file -> List.of();
        assertInstanceOf(ResolvedModel.class, resolver.resolve(scene(), "m.bin"));
        assertEquals(2, extractor.calls);
    }

    @Test
    void aModelThatReferencesItselfThroughAnotherIsACycle() throws Exception {
        touch("a.bin");
        touch("b.bin");
        Path scene = scene();
        CachingModelResolver resolver = resolver();
        List<ModelResult> nested = new ArrayList<>();
        extractor.script = file -> {
            if (file.getFileName().toString().equals("a.bin")) {
                nested.add(resolver.resolve(scene, "b.bin"));
            } else {
                nested.add(resolver.resolve(scene, "a.bin"));
            }

            return List.of();
        };

        resolver.resolve(scene, "a.bin");

        BrokenReference loop = assertInstanceOf(BrokenReference.class, nested.get(0));
        assertEquals(Reason.CYCLE, loop.reason());
        assertTrue(resolver.wouldCycle(root.resolve("a.bin"), root.resolve("b.bin")));
    }

    @Test
    void deeplyNestedReferencesStopAtTheDepthLimit() throws Exception {
        for (int i = 0; i <= CachingModelResolver.MAX_DEPTH + 2; i++) {
            touch("m" + i + ".bin");
        }
        Path scene = scene();
        CachingModelResolver resolver = resolver();
        List<ModelResult> deepest = new ArrayList<>();
        extractor.script = file -> {
            String name = file.getFileName().toString();
            int index = Integer.parseInt(name.substring(1, name.indexOf('.')));
            ModelResult next = resolver.resolve(scene, "m" + (index + 1) + ".bin");

            if (next instanceof BrokenReference) {
                deepest.add(next);
            }

            return List.of();
        };

        resolver.resolve(scene, "m0.bin");

        BrokenReference stop = assertInstanceOf(BrokenReference.class, deepest.get(0));
        assertEquals(Reason.TOO_DEEP, stop.reason());
    }

    @Test
    void wouldCycleFollowsKnownReferences() throws Exception {
        Path a = touch("a.bin");
        Path b = touch("b.bin");
        Path scene = scene();
        CachingModelResolver resolver = resolver();
        extractor.script = file -> {
            if (file.equals(a)) {
                resolver.resolve(scene, "b.bin");
            }

            return List.of();
        };

        resolver.resolve(scene, "a.bin");

        // a refers to b, so b referring to a would close a loop and the other way round would not
        assertTrue(resolver.wouldCycle(b, a));
        assertFalse(resolver.wouldCycle(a, b));
        assertTrue(resolver.wouldCycle(a, a));
        assertFalse(resolver.wouldCycle(root.resolve("x.bin"), root.resolve("y.bin")));
    }

    @Test
    void realGltfFilesResolveThroughTheGltfExtractor() throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(36).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : new float[] { 0, 0, 0, 1, 0, 0, 0, 1, 0 }) {
            buffer.putFloat(value);
        }
        String json = "{\"asset\":{\"version\":\"2.0\"},"
            + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0}}]}],"
            + "\"buffers\":[{\"uri\":\"data:application/octet-stream;base64,"
            + Base64.getEncoder().encodeToString(buffer.array()) + "\",\"byteLength\":36}],"
            + "\"bufferViews\":[{\"buffer\":0,\"byteLength\":36}],"
            + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":3,\"type\":\"VEC3\"}]}";
        Files.writeString(root.resolve("tri.gltf"), json);
        CachingModelResolver resolver = new CachingModelResolver(new GltfMeshExtractor(), root);

        ModelResult result = resolver.resolve(scene(), "tri.gltf");

        ResolvedModel model = assertInstanceOf(ResolvedModel.class, result);
        assertEquals(1, model.parts().size());
        assertEquals(1f, model.bounds().getMaxX(), EPS);
    }

}
